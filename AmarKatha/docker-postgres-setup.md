# PostgreSQL Docker Setup for AmarKatha

This guide will help you set up PostgreSQL in Docker for your AmarKatha project.

## Prerequisites

- Docker installed on your system
- Docker Compose installed
- Git (to clone the repository)

## Quick Setup Instructions

### 1. Start PostgreSQL Container

```bash
# Start the PostgreSQL container
docker-compose up -d postgres

# Check if the container is running
docker-compose ps

# View logs to ensure it started successfully
docker-compose logs postgres
```

### 2. Verify Database Connection

```bash
# Connect to the database using psql (if you have it installed)
psql -h localhost -p 5432 -U amarkatha_user -d amarkatha

# Or connect using Docker
docker exec -it amarkatha_postgres psql -U amarkatha_user -d amarkatha
```

### 3. Update Your Flask Configuration

Create or update your `.env` file:

```bash
# Database configuration
DATABASE_URL=postgresql://amarkatha_user:amarkatha_password@localhost:5432/amarkatha

# Other Flask settings
SECRET_KEY=your-secret-key-here
FLASK_ENV=development
```

### 4. Install PostgreSQL Python Dependencies

```bash
# Activate your virtual environment
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install PostgreSQL adapter
pip install psycopg2-binary

# Update requirements.txt
pip freeze > requirements.txt
```

### 5. Initialize Database

```bash
# Initialize the database with your Flask models
flask init-db

# Create admin user
flask create-admin
```

## Database Connection Details

- **Host**: localhost
- **Port**: 5432
- **Database**: amarkatha
- **Username**: amarkatha_user
- **Password**: amarkatha_password

## Useful Docker Commands

### Start/Stop Services
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: This will delete all data)
docker-compose down -v

# Restart PostgreSQL only
docker-compose restart postgres
```

### Database Management
```bash
# Access PostgreSQL shell
docker exec -it amarkatha_postgres psql -U amarkatha_user -d amarkatha

# Backup database
docker exec amarkatha_postgres pg_dump -U amarkatha_user amarkatha > backup.sql

# Restore database
docker exec -i amarkatha_postgres psql -U amarkatha_user -d amarkatha < backup.sql

# View database logs
docker-compose logs postgres

# Follow logs in real-time
docker-compose logs -f postgres
```

### Container Management
```bash
# Check container status
docker-compose ps

# View resource usage
docker stats amarkatha_postgres

# Remove container and start fresh
docker-compose down
docker-compose up -d postgres
```

## Troubleshooting

### Common Issues

1. **Port 5432 already in use**
   ```bash
   # Check what's using the port
   lsof -i :5432
   
   # Stop the conflicting service or change the port in docker-compose.yml
   ```

2. **Permission denied errors**
   ```bash
   # Ensure Docker has proper permissions
   sudo usermod -aG docker $USER
   # Log out and log back in
   ```

3. **Database connection refused**
   ```bash
   # Check if container is running
   docker-compose ps
   
   # Check container logs
   docker-compose logs postgres
   
   # Restart the container
   docker-compose restart postgres
   ```

4. **Data persistence issues**
   ```bash
   # Check if volume is created
   docker volume ls | grep postgres_data
   
   # Inspect volume
   docker volume inspect amarkatha_postgres_data
   ```

### Reset Everything

If you need to start completely fresh:

```bash
# Stop and remove everything
docker-compose down -v

# Remove any leftover containers
docker rm -f amarkatha_postgres

# Remove any leftover volumes
docker volume rm amarkatha_postgres_data

# Start fresh
docker-compose up -d postgres
```

## Production Considerations

For production deployment:

1. **Change default passwords**
2. **Use environment variables for sensitive data**
3. **Set up proper backup strategies**
4. **Configure SSL connections**
5. **Set up monitoring and logging**
6. **Use a managed PostgreSQL service (AWS RDS, Google Cloud SQL, etc.)**

## Environment Variables for Production

Create a `.env` file for production:

```bash
POSTGRES_DB=amarkatha_prod
POSTGRES_USER=amarkatha_prod_user
POSTGRES_PASSWORD=your_secure_password_here
POSTGRES_HOST=your_production_host
POSTGRES_PORT=5432
```

Update `docker-compose.yml` to use these variables:

```yaml
environment:
  POSTGRES_DB: ${POSTGRES_DB}
  POSTGRES_USER: ${POSTGRES_USER}
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
```

## Next Steps

1. Update your Flask models to use PostgreSQL-specific features
2. Set up database migrations using Flask-Migrate
3. Configure connection pooling for better performance
4. Set up automated backups
5. Monitor database performance

## Additional Resources

- [PostgreSQL Docker Official Image](https://hub.docker.com/_/postgres)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Flask-SQLAlchemy PostgreSQL](https://flask-sqlalchemy.palletsprojects.com/en/2.x/config/)
- [psycopg2 Documentation](https://www.psycopg.org/docs/) 