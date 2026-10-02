import os
import io
import unittest
from PIL import Image
from fastapi.testclient import TestClient
import app.server as server

class TestServerAPI(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # locate ourselves on disk
        BASE_DIR = os.path.dirname(os.path.abspath(__file__))
        # where your model lives
        cls.model_path = os.path.join(BASE_DIR, "..", "rebar_yolov8s_100_epochs.pt")
        cls.device = "cpu"
        # pick one real sample image (must exist)
        cls.sample_image = os.path.join(
            BASE_DIR, "..", "sample_images",
            "company_rebar_3_jpg.rf.cd16bf4815b477c64892facae01169e4.jpg"
        )

        # override YOLO to avoid heavy compute
        def fake_predict(img, *args, **kwargs):
            # pretend it always finds zero rebars
            return []
        server.model.predict = fake_predict

        cls.client = TestClient(server.app)

    def test_tc_wb_04_valid_jpeg(self):
        """TC-WB-04: valid JPEG upload returns 200 + JSON with rebarCount and annotated_image"""
        with open(self.sample_image, "rb") as f:
            data = f.read()

        files = {"image": ("rebar.jpg", data, "image/jpeg")}
        resp = self.client.post("/count/", files=files)
        self.assertEqual(resp.status_code, 200, resp.text)

        j = resp.json()
        # basic shape assertions
        self.assertIn("rebarCount", j)
        self.assertIn("annotated_image", j)
        self.assertEqual(j["rebarCount"], 0)
        self.assertTrue(j["annotated_image"].startswith("data:image/jpeg;base64,"))

        # print the successful results
        print(f"TC-WB-04 rebarCount: {j['rebarCount']}")
        snippet = j["annotated_image"][0:80] + "..."
        print(f"TC-WB-04 annotated_image (base64 snippet): {snippet}")

    def test_tc_wb_05_missing_image_field(self):
        """TC-WB-05: missing image key => 422 validation error"""
        resp = self.client.post("/count/", files={})
        self.assertEqual(resp.status_code, 422)
        body = resp.json()
        # ensure FastAPI reports "field required"
        self.assertTrue(any(err["msg"] == "field required" for err in body["detail"]))

    def test_tc_wb_06_unsupported_bmp(self):
        """TC-WB-06: .bmp upload => 400 Bad Request about content type"""
        # build a tiny BMP in memory
        buf = io.BytesIO()
        Image.new("RGB", (1,1), (0,128,0)).save(buf, format="BMP")
        buf.seek(0)

        files = {"image": ("rebar.bmp", buf.read(), "image/bmp")}
        resp = self.client.post("/count/", files=files)
        self.assertEqual(resp.status_code, 400, resp.text)
        self.assertEqual(resp.json()["detail"], "Only PNG or JPEG images are allowed")

if __name__ == "__main__":
    unittest.main()
