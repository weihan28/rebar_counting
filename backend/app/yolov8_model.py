# app/yolov8_model.py
from ultralytics import YOLO
import torch

class YOLORebarCounter:
    def __init__(self, model_path: str, device: str = "cpu"):
        self.device = device
        # load once at startup (supports .pt or TorchScript)
        self.model = YOLO(model_path)

    def predict(self, img, conf: float = 0.50, iou: float = 0.50):
        """
        img: PIL.Image or file path
        returns: list of dicts each with x1,y1,x2,y2,confidence,class
        """
        results = self.model(img, device=self.device, conf=conf, iou=iou)
        res0 = results[0]
        # each box: [x1, y1, x2, y2, confidence, cls]
        boxes = res0.boxes.data.cpu().tolist()
        preds = []
        for x1, y1, x2, y2, score, cls in boxes:
            preds.append({
                "x1": x1, "y1": y1,
                "x2": x2, "y2": y2,
                "confidence": float(score),
                "class": int(cls)
            })
        return preds