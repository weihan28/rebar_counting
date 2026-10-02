# Rebar Counting

Count steel rebars from a photo. Point your phone at a bundle of rebar, snap a picture, and get back the count along with an annotated image showing every detected bar.

This is our final year project. It has two parts: a YOLOv8-based inference API, and an Android app that uses it.

```text
┌──────────────────┐   photo (POST /count/)   ┌─────────────────────────┐
│   Android app    │ ───────────────────────▶ │  FastAPI backend        │
│  Kotlin/Compose  │                          │  YOLOv8 rebar detector  │
│  Firebase        │ ◀─────────────────────── │  Docker → Render        │
└──────────────────┘  count + annotated image └─────────────────────────┘
```

## Repository layout

| Folder | What it is | Details |
| --- | --- | --- |
| [`backend/`](backend/) | FastAPI service that serves our trained YOLOv8 model. It takes an image and returns the rebar count and an annotated copy. Dockerized, with CI/CD to Render. | [backend/README.md](backend/README.md) |
| [`mobile_app/`](mobile_app/) | Android app (Kotlin, Jetpack Compose). It captures photos, calls the API, and keeps a history of counts. | [mobile_app/](mobile_app/) |
| [`report.pdf`](report.pdf) | The project report. | |

## How it works

1. The user takes or picks a photo in the app.
2. The app uploads it to the backend as a multipart request to `POST /count/`.
3. The backend runs the YOLOv8 model (confidence threshold 0.50, IoU 0.50), draws a box around each detected bar, and counts the detections.
4. The backend responds with:

   ```json
   {
     "rebarCount": 12,
     "annotated_image": "data:image/jpeg;base64,..."
   }
   ```

5. The app shows the annotated result and saves it to the user's history.

## Backend

Python 3.10, FastAPI, and Ultralytics YOLOv8. The trained weights (`rebar_yolov8s_100_epochs.pt`) are included in the repo.

On the 135-image validation set the model reaches precision 0.983, recall 0.971, mAP@0.50 0.988, and 96.82% counting accuracy (MAE 4.39 rebar/image). See [Model Performance](backend/README.md#model-performance) for the full tables.

Quick start:

```bash
cd backend
python3.10 -m venv venv && source venv/bin/activate
pip install -r requirements.txt
uvicorn app.server:app --reload
```

Then open <http://localhost:8000/docs> to try the API. The backend README also covers Docker, tests, and the GitHub Actions → Docker Hub → Render pipeline.

**Go to [`backend/`](backend/README.md) for setup, API details, and deployment.**

## Mobile app

Android app built with Kotlin and Jetpack Compose. Min SDK 30, target SDK 35. It includes:

- Onboarding and sign-in (Firebase Auth)
- Photo capture and upload, with a processing screen while the model runs
- Annotated result view
- Count history and a dashboard with a trend graph (Firestore and Firebase Storage)
- User profile

The app calls the hosted backend at `https://rebar-count.onrender.com`. To point it at a local or different server, change `baseUrl` in [`RetrofitProvider.kt`](mobile_app/app/src/main/java/com/fyp/rebarcountingapp/network/RetrofitProvider.kt).

To run it, open `mobile_app/` in Android Studio, let Gradle sync, and run on an emulator or device.

**Go to [`mobile_app/`](mobile_app/) for the source. The screens live in [`app/src/main/java/com/fyp/rebarcountingapp/`](mobile_app/app/src/main/java/com/fyp/rebarcountingapp/).**

> **Note:** The hosted backend runs on Render, so the first request after a period of inactivity can be slow while the service wakes up. The app allows a 5-minute read timeout for this reason.

## Team

- AI model training: Chin Wei Han
- Mobile app integration: Chew Chee Lel
- Project coordination: Tan Wei Ming
- Quality testing: Ong Chang Le
