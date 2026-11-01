import asyncio
import os
import wave

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

    async def text_to_speech(self, text: str) -> str:
        final_path = os.path.join(self.output_dir, self.filename)
        tmp_mp3 = os.path.join(self.output_dir, "tmp_resposta.mp3")
        tmp_wav = final_path + ".tmp"

        async with self._lock:
            communicate = edge_tts.Communicate(text, self.default_voice)
            await communicate.save(tmp_mp3)

            await asyncio.to_thread(self._mp3_para_wav, tmp_mp3, tmp_wav)
            os.replace(tmp_wav, final_path)
            os.remove(tmp_mp3)

        return final_path