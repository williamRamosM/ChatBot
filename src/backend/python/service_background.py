import logging
from functools import cache
from io import BytesIO
from threading import Lock

from fastapi import APIRouter, HTTPException, Request, Response
from starlette.concurrency import run_in_threadpool


router = APIRouter()
logger = logging.getLogger(__name__)
session_lock = Lock()


@cache
def background_session():
    from rembg import new_session

    return new_session("u2netp", providers=["CPUExecutionProvider"])


def cut_background(image: bytes) -> bytes:
    from PIL import Image
    from rembg import remove

    try:
        with Image.open(BytesIO(image), formats=["PNG"]) as original:
            original.load()
    except (OSError, ValueError, Image.DecompressionBombError) as error:
        raise HTTPException(422, "Não foi possível ler o PNG recebido.") from error

    with session_lock:
        session = background_session()
    return remove(image, session=session)


@router.post("/remove-background")
async def remove_background(request: Request):
    image = await request.body()
    if len(image) > 16 * 1024 * 1024:
        raise HTTPException(413, "A imagem excede 16 MB.")
    if not image.startswith(b"\x89PNG\r\n\x1a\n"):
        raise HTTPException(415, "Envie uma imagem PNG.")

    try:
        output = await run_in_threadpool(cut_background, image)
    except ImportError as error:
        raise HTTPException(503, 'Instale o rembg na venv com: python -m pip install "rembg[cpu]"') from error
    except HTTPException:
        raise
    except Exception as error:
        logger.exception("Falha na remoção local de fundo")
        raise HTTPException(500, "Falha ao remover o fundo. Confira o terminal do backend.") from error

    return Response(output, media_type="image/png")
