FROM python:3.10-slim

# Install OpenCV dependencies
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0 \
 && rm -rf /var/lib/apt/lists/*

# Set workdir
WORKDIR /usr/src/app

# Copy requirements and install
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy rest of the code
COPY . .

# Run the app
CMD ["uvicorn", "app.server:app", "--host", "0.0.0.0", "--port", "8000"]
