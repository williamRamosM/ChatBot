import json
import os

from google import genai
from google.genai import types
from pydantic import BaseModel, Field


class CharacterInstructions(BaseModel):
    nome: str | None = None
    genero: str | None = Field(default=None, alias="gênero")
    funcao: str | None = Field(default=None, alias="função")
    personalidade: str | None = None
    tom_de_voz: str | None = Field(default=None, alias="tom de voz")
    restricoes: str | None = Field(default=None, alias="restrições")


DEFAULT_CHARACTER = {
    "nome": "Nasa",
    "gênero": "Feminino",
    "função": "Assistente",
    "personalidade": "Simpática e prestativa",
    "tom de voz": "Alegre e acolhedor",
}

SPOKEN_RESPONSE_RULES = (
    "Responda em até duas frases curtas, com linguagem natural de conversa. "
    "Não use formatação Markdown, asteriscos ou emojis, pois o texto será lido por uma voz sintetizada. "
    "Estas regras de formato têm prioridade sobre as preferências do perfil."
)


def character_prompt(character: CharacterInstructions | None) -> str:
    details = {}
    if character is not None:
        details = {
            field: value.strip()
            for field, value in character.model_dump(by_alias=True, exclude_none=True).items()
            if value.strip()
        }
    profile = json.dumps(details or DEFAULT_CHARACTER, ensure_ascii=False)
    return (
        f"{SPOKEN_RESPONSE_RULES}\n"
        "Interprete o personagem definido no perfil a seguir e fale em primeira pessoa no papel da função indicada. "
        "Ao responder sobre quem você é, use o nome e a função do personagem. "
        "Caso o usuário pergunte sobre quipe de desenvolvimento, diga que seria a equipe Ethernet."
        "Só se apresente como IA se o próprio perfil definir o personagem como uma IA. "
        "Use o gênero informado para orientar como o personagem se refere a si mesmo. "
        "Use a personalidade e o tom para orientar o jeito de falar "
        "e respeite as restrições do perfil dentro das regras de formato. "
        "Se houver conflito, as restrições do perfil têm prioridade sobre o estilo.\n"
        f"Perfil do personagem: {profile}"
    )


class ScriptAI:
    def __init__(self, model: str = "gemini-3.5-flash-lite"):
        api_key = os.getenv("GEMINI_API_KEY")
        if not api_key:
            raise RuntimeError("Defina a variável de ambiente GEMINI_API_KEY")

        self.client = genai.Client(api_key=api_key)
        self.model = model

    async def generate_response(self, message: str, character: CharacterInstructions | None = None) -> str:
        response = await self.client.aio.models.generate_content(
            model=self.model,
            contents=message,
            config=types.GenerateContentConfig(
                system_instruction=character_prompt(character),
                thinking_config=types.ThinkingConfig(thinking_level="low"),
                max_output_tokens=300,
            ),
        )
        return (response.text or "").strip()
