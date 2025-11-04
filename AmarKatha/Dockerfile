# Use Python 3.11 slim image
FROM python:3.11-slim

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV FLASK_APP=run.py
ENV FLASK_ENV=production

# Set work directory
WORKDIR /app

# Install system dependencies
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        gcc \
        libpq-dev \
        libjpeg-dev \
        libpng-dev \
        libgif-dev \
        openssl \
        curl \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy project
COPY . .

# Copy Google credentials if they exist (will be ignored if not present)
COPY google_credentials.json* ./
COPY credentials.json* ./
COPY client_secret.json* ./

# Copy SSL certificates if they exist
COPY ssl/ ./ssl/

# Create uploads directory
RUN mkdir -p app/static/uploads

# Create non-root user
RUN adduser --disabled-password --gecos '' appuser
RUN chown -R appuser:appuser /app
USER appuser

# Expose ports for both HTTP and HTTPS
EXPOSE 5000 5002

# Health check
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:5000/health || exit 1

# Run the application with both HTTP and HTTPS
CMD ["python", "start_server.py"] 