import asyncio
import os
import wave
from pathlib import Path

import edge_tts
import miniaudio


class TTSService:
    def __init__(
        self,
        output_dir: str = "static",
        default_voice: str = "pt-BR-FranciscaNeural",
        filename: str = "resposta.wav",
    ):
        self.output_dir = output_dir
        self.default_voice = default_voice
        self.filename = filename
        self._lock = asyncio.Lock()
        os.makedirs(self.output_dir, exist_ok=True)

    @staticmethod
    def _mp3_para_wav(mp3_path: str, wav_path: str) -> None:
        decoded = miniaudio.decode_file(
            mp3_path, output_format=miniaudio.SampleFormat.SIGNED16
        )
        with wave.open(wav_path, "wb") as w:
            w.setnchannels(decoded.nchannels)
            w.setsampwidth(2)  
            w.setframerate(decoded.sample_rate)
            w.writeframes(decoded.samples.tobytes())

    async def text_to_speech(self, text: str, voice_id: str | None = None) -> str:
        return await self._synthesize(text, voice_id or self.default_voice, self.filename)

    async def preview_voice(self, voice_id: str | None = None) -> str:
        voice = voice_id or self.default_voice
        return await self._synthesize(
            "Olá! Esta é uma amostra da minha voz.", voice, f"previa-{voice}.wav", cached=True
        )

    async def _synthesize(self, text: str, voice: str, filename: str, *, cached: bool = False) -> str:
        final_path = Path(self.output_dir) / filename
        tmp_mp3 = final_path.with_name(final_path.name + ".mp3")
        tmp_wav = final_path.with_name(final_path.name + ".tmp")

        async with self._lock:
            if cached and final_path.is_file():
                return str(final_path)
            try:
                communicate = edge_tts.Communicate(text, voice)
                await communicate.save(str(tmp_mp3))
                await asyncio.to_thread(self._mp3_para_wav, str(tmp_mp3), str(tmp_wav))
                os.replace(tmp_wav, final_path)
            finally:
                tmp_mp3.unlink(missing_ok=True)
                tmp_wav.unlink(missing_ok=True)

        return str(final_path)
