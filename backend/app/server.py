from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from app.yolov8_model import YOLORebarCounter
import io
from PIL import Image
import logging
import os
import cv2
import base64
import numpy as np

import torchvision.transforms as T

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# Use absolute path for model file
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "..", "rebar_yolov8s_100_epochs.pt")

# Instantiate YOLO model
model = YOLORebarCounter(MODEL_PATH, device="cpu")

def draw_image(pilImage: Image.Image, prediction):
    npimg = np.array(pilImage)
    bgr = cv2.cvtColor(npimg, cv2.COLOR_RGB2BGR)
    for p in prediction:
        x1,y1,x2,y2 = map(int, (p["x1"], p["y1"], p["x2"], p["y2"]))
        cv2.rectangle(bgr, (x1,y1), (x2,y2), (0,0,255), 2)
        text = f"{p['confidence']:.2f}"
        cv2.putText(bgr, text, (x1, y1-6),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.4, (0,0,255), 1)

    # 4) re-encode to JPEG and base64
    ok, buf = cv2.imencode(".jpg", bgr)
    return ok, buf

@app.post("/count/")
async def count(image: UploadFile = File(...)):
    logger.info(f"Received request to /count/ with file: {image.filename}, content-type: {image.content_type}")

    # Validate content type
    if image.content_type not in ["image/png", "image/jpeg", "image/jpg"]:
        logger.error(f"Invalid content type: {image.content_type}")
        raise HTTPException(status_code=400, detail="Only PNG or JPEG images are allowed")

    try:
        img_bytes = await image.read()
        logger.info("Decoding image")
        pilImg = Image.open(io.BytesIO(img_bytes)).convert("RGB")
        logger.info("Image decoded successfully")

        logger.info("Running YOLO prediction")
        prediction = model.predict(pilImg)
        rebar_count = len(prediction)
        logger.info(f"Prediction complete, rebar count: {rebar_count}")
        ok, buf = draw_image(pilImg,prediction)

        if not ok:
            raise HTTPException(500, "Failed to encode image")
        b64 = base64.b64encode(buf.tobytes()).decode()

        return {
            "rebarCount": rebar_count,
            "annotated_image": f"data:image/jpeg;base64,{b64}"
            # "boxes": preds,
        }
    except Exception as e:
        logger.error(f"Error processing image: {str(e)}")
        raise HTTPException(status_code=400, detail=f"Invalid image file: {str(e)}")


@app.get("/")
async def root():
    logger.info("Received request to /")
    return {"status": "Rebar API running"}