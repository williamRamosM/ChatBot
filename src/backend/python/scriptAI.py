import os
from google import genai
from google.genai import types


class LunaAI:
    def __init__(self, model: str = "gemini-3.5-flash-lite"):
        api_key = os.getenv("GEMINI_API_KEY")
        if not api_key:
            raise RuntimeError("Defina a variável de ambiente GEMINI_API_KEY")

        self.client = genai.Client(api_key=api_key)
        self.model = model
        self.system_prompt = """
        Você é a 'Nasa', uma assistente robótica do futuro muito simpatica e prestativa.
        Sempre responda de forma alegre e confortavel.
        Mantenha as respostas bem curtas (máximo de 2 frases).
        Não use formatação markdown, asteriscos ou emojis, pois o texto será lido por uma voz sintetizada.
        """

    async def generate_response(self, message: str) -> str:
        response = await self.client.aio.models.generate_content(
            model=self.model,
            contents=message,
            config=types.GenerateContentConfig(
                system_instruction=self.system_prompt,
                thinking_config=types.ThinkingConfig(thinking_level="low"),
                max_output_tokens=300,
            ),
        )
        return (response.text or "").strip()