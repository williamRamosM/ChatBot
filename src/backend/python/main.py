import logging
import os
import time
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()  # precisa vir antes de criar a LunaAI

from fastapi import FastAPI, HTTPException
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from scriptAI import ScriptAI
from service_avatar import router as avatar_router
from service_background import router as background_router
from service_tts import TTSService

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

BASE_DIR = Path(__file__).resolve().parent
STATIC_DIR = BASE_DIR / "static"
STATIC_DIR.mkdir(exist_ok=True)

app = FastAPI()
app.include_router(avatar_router)
app.include_router(background_router)
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

luna = ScriptAI()
tts_service = TTSService(output_dir=str(STATIC_DIR))


class ChatRequest(BaseModel):
    message: str


@app.get("/")
def root():
    return {"message": "Ah, claro, mais um humano visitando a raiz. Use a rota /docs se quiser interagir de verdade."}


@app.post("/generate-voice-chat")
async def generate_voice(request: ChatRequest):
    try:
        t0 = time.perf_counter()
        bot_text = await luna.generate_response(request.message)
        logger.info("Gemini levou %.1fs", time.perf_counter() - t0)

        if not bot_text:
            raise HTTPException(status_code=502, detail="A IA não retornou uma resposta.")

        t1 = time.perf_counter()
        audio_path = await tts_service.text_to_speech(bot_text)
        logger.info("TTS levou %.1fs", time.perf_counter() - t1)

        audio_url = f"/static/{os.path.basename(audio_path)}"

        return {"text_response": bot_text, "audio_url": audio_url}

    except HTTPException:
        raise
    except Exception:
        logger.exception("Erro ao gerar resposta de voz")
        raise HTTPException(status_code=500, detail="Erro interno ao processar a requisição.")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8000)
