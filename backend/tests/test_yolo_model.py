import unittest
import torch
import os
from app.yolov8_model import YOLORebarCounter
from torchvision.transforms import ToPILImage
from PIL import Image

class TestYOLORebarCounter(unittest.TestCase):
    def setUp(self):
        # Use absolute path for model file
        BASE_DIR = os.path.dirname(os.path.abspath(__file__))

        # valid tensor
        self.model_path = os.path.join(BASE_DIR, "..", "rebar_yolov8s_100_epochs.pt")
        self.device = "cpu"
        self.sample_image = os.path.join(BASE_DIR, "..", "sample_images", "company_rebar_3_jpg.rf.cd16bf4815b477c64892facae01169e4.jpg")

        # random_img_pil
        img_tensor = torch.rand(3, 1000, 1000)  # Shape: (C, H, W)
        to_pil = ToPILImage()
        self.random_img_pil = to_pil(img_tensor)


    # White-box Testing 1: YOLOv8 Model Inference Logic
    def test_yolo_model_initialize(self):
        """Test that the YOLO model loads without error."""
        try:
            model = YOLORebarCounter(self.model_path, device=self.device)
            self.assertIsNotNone(model.model, "Model should be loaded")
            print("Model Successfully Initialized without Error")
            print(model.model)
        except Exception as e:
            self.fail(f"Model loading failed: {str(e)}")

    # White-box Testing 2: YOLOv8 Model Inference Logic
    def test_yolo_predict_integer_output(self):
        """Test that predict returns an integer count for a sample image tensor."""
        model = YOLORebarCounter(self.model_path, device=self.device)

        img_pil = Image.open(self.sample_image).convert("RGB")

        pred = model.predict(img_pil)
        print(pred)
        self.assertIsInstance(pred, list, "Prediction should be a list")
        self.assertGreater(len(pred), 0, "Prediction for this image should not be empty")


    def test_yolo_predict_invalid_input(self):
        """Test that predict returns an integer count for a sample image tensor."""
        # invalid_input_tensor
        img_tensor = torch.rand(3, 100, 100)  # Shape: (C, H, W)

        model = YOLORebarCounter(self.model_path, device=self.device)
        with self.assertRaises(ValueError):
            pred = model.predict(img_tensor)
            print(pred)

        print("Successfully Raised ValueError")






if __name__ == "__main__":
    unittest.main()