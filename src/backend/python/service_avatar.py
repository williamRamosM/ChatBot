import base64
import os

import httpx
from fastapi import APIRouter, HTTPException, Request, Response


router = APIRouter()
MODEL = "@cf/black-forest-labs/flux-2-klein-4b"
PROMPT = (
    "Edit only the mouth so the same character appears to be speaking with its mouth open. "
    "Keep the character's identity, style, pose, scale, position, framing, and background. "
    "Do not zoom, crop, move the character, or add any text."
)


def cloudflare_error(response: httpx.Response, account: str, token: str) -> str:
    summary = f"A Cloudflare rejeitou a geração (HTTP {response.status_code})"
    try:
        error = response.json()["errors"][0]
    except (ValueError, KeyError, IndexError, TypeError):
        return summary + "."
    if not isinstance(error, dict):
        return summary + "."

    code = error.get("code")
    if isinstance(code, int):
        summary += f", código {code}"
    message = error.get("message")
    if not isinstance(message, str) or not message.strip():
        return summary + "."

    message = message.replace(token, "[oculto]").replace(account, "[oculto]")
    return f"{summary}: {' '.join(message.split())[:300]}"


@router.post("/generate-speaking-image")
async def generate_speaking_image(request: Request):
    account = os.getenv("CLOUDFLARE_ACCOUNT_ID")
    token = os.getenv("CLOUDFLARE_API_TOKEN")
    if not account or not token:
        raise HTTPException(503, "Configure CLOUDFLARE_ACCOUNT_ID e CLOUDFLARE_API_TOKEN no .env.")

    image = await request.body()
    if len(image) > 2 * 1024 * 1024:
        raise HTTPException(413, "A imagem de referência excede 2 MB.")
    if not image.startswith(b"\x89PNG\r\n\x1a\n"):
        raise HTTPException(415, "A imagem de referência precisa estar em PNG.")

    url = f"https://api.cloudflare.com/client/v4/accounts/{account}/ai/run/{MODEL}"
    try:
        async with httpx.AsyncClient(timeout=75) as client:
            result = await client.post(
                url,
                headers={"Authorization": f"Bearer {token}"},
                data={"prompt": PROMPT, "width": "512", "height": "512"},
                files={"input_image_0": ("personagem.png", image, "image/png")},
            )
            if result.status_code == 429:
                raise HTTPException(429, "A cota de geração do Cloudflare está indisponível no momento.")
            if result.status_code in (401, 403):
                raise HTTPException(502, "Confira a conta e as permissões do token do Cloudflare Workers AI.")
            if not result.is_success:
                raise HTTPException(502, cloudflare_error(result, account, token))
            output = base64.b64decode(result.json()["result"]["image"], validate=True)
            if not output:
                raise ValueError("Imagem vazia.")
    except httpx.TimeoutException as error:
        raise HTTPException(504, "O Cloudflare demorou para gerar a imagem.") from error
    except httpx.RequestError as error:
        raise HTTPException(502, "Não foi possível conectar à Cloudflare.") from error
    except (ValueError, KeyError, TypeError) as error:
        raise HTTPException(502, "O Cloudflare não retornou uma imagem válida.") from error

    return Response(output, media_type="application/octet-stream")
