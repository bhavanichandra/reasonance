# Reasonance

## Multi-Turn Reasoning That Evolves, Not Responds

**Reasonance is not a chatbot.**

It is a **turn-based reasoning engine** where Gemini maintains and evolves a persistent hypothesis graph across multiple investigative turns, with explicit memory, cross-references, and final synthesis.

Instead of asking Gemini a question and getting an answer, Reasonance creates an environment where:

> Gemini **thinks over time**.

## The Problem

Most LLM applications are single-turn or short-context interactions:

* Prompt → Response → Done
* Chat history as memory
* No visible reasoning state
* No evolution of thought

But real reasoning — the kind used in research, investigations, philosophy, and science — happens across **multiple iterative passes**, where hypotheses are:

* formed
* tested against evidence
* mutated
* cross-referenced
* synthesized

## What Reasonance Does

Reasonance turns Gemini into a **persistent abductive reasoning partner**.

You don’t chat with it.
You **conduct an investigation with it**.

Each investigation proceeds across multiple **turns**:

1. Upload ambiguous material (case file, paradox, document, theory, etc.)
2. Gemini generates initial key insights + signals
3. You choose which insight to pursue and which signal to test
4. Gemini performs a deep abductive dive and mutates the reasoning graph
5. New insights and c are generated with references to prior turns
6. Repeat for multiple turns
7. Gemini performs a final synthesis across the entire reasoning lineage

At the end, you get a **complete investigation archive** showing how thought evolved.

## The Board Is Gemini’s Working Memory

The UI is not a layout.
It is a visualization of Gemini’s cognitive state.

* Left panel → All insights across turns
* Center → Active insight under investigation
* Bottom → signal pool for reasoning
* Top → Turn navigation through reasoning history

You are literally looking at the **AI’s evolving hypothesis graph**.

## The Reasoning Lifecycle

```
Source Material
      ↓
Turn 1: Initial insights + signals
      ↓
Human selects direction
      ↓
Gemini deep dive (What-If hypothesis)
      ↓
Graph mutation (new insights + signal + references)
      ↓
Next Turn built from entire history
      ↓
Final synthesis over full reasoning lineage
```

This is **not chat**.
This is **orchestrated reasoning**.

## Example Use Cases

* Analyzing paradoxes in physics or mathematics
* Investigating case documents or witness statements
* Exploring philosophical or religious texts across perspectives
* Generating scientific insights from ambiguous data
* Research and academic analysis

## Architecture

![Design](./docs/reasonance-design.svg)

This is a **reasoning state machine**, not an API wrapper.

## 🛠️ Tech Stack

* **Backend**: Spring Boot, PostgreSQL
* **Frontend**: Angular
* **LLM**: Gemini 3 / Gemma3n via Ollama
* **Graph Model**: Nodes (insights/signal) + edges (relationships)

Full developer documentation is in `/docs`.

## 🎥 Demo

👉 *[Your 3-minute demo video link]*

## Usage

This document provides comprehensive instructions for running the Reasonance application using Docker Compose, including environment configuration, setup steps, and troubleshooting.

### Overview

The Reasonance application is now fully containerized with the following services:

* **PostgreSQL Database** (pgvector): Stores all application data with vector search capabilities
* **RustFS**: S3-compatible object storage for files
* **Backend API** (Java 25 / Spring Boot 4): RESTful API server
* **Frontend** (Angular 21): Web UI served by Nginx
* **Ollama**: Local LLM inference engine with gemma3n model

### Quick Start

#### Prerequisites

* Docker Engine 20.10+
* Docker Compose 2.0+
* 8GB+ available RAM (Ollama model requires significant memory)
* 50GB+ disk space (for Ollama models)

#### 1. Clone or Update Repository

```bash
cd reasonance
```

#### 2. Configure Environment Variables

The `.env` file contains default configuration values. Create `.env.local` for any local overrides:

```bash
# Copy the example env file and customize
cp .env.example .env       # On Windows: copy .env.example .env

# IMPORTANT: If you plan to use Google Gemini as the LLM provider,
# set `GEMINI_API_KEY` in `.env` before starting the stack. This is
# a prerequisite for Gemini-based operation.
```

#### 3. Start All Services

```bash
# Build all images and start services
docker-compose up --build

# Or run in background
docker-compose up -d --build
```

#### 4. Initialize Ollama Model (First Run)

On first startup, Ollama needs to download and initialize the gemma3n model:

```bash
# Access Ollama container
docker exec -it reasonance-ollama ollama pull gemma3n
```

#### 5. Verify Services

```bash
# Check all services are running
docker-compose ps

# View logs
docker-compose logs -f

# Check specific service
docker-compose logs backend
docker-compose logs frontend
```

#### 6. Access Application

* **Frontend**: <http://localhost>
* **Backend API**: <http://localhost:8080/api/v1>
* **API Documentation**: <http://localhost:8080/swagger-ui.html>
* **RustFS Console**: <http://localhost:9001> (access key: reasonance_admin)
* **Ollama API**: <http://localhost:11434>

### Environment Configuration

#### .env File Structure

The `.env` file (in repository) contains default values for all services:

```env
# Database
POSTGRES_DB=reasonance_db
POSTGRES_USER=reasonance_user
POSTGRES_PASSWORD=reasonance_password

# RustFS Storage
RUSTFS_ACCESS_KEY=reasonance_admin
RUSTFS_SECRET_KEY=reasonance_admin
RUSTFS_BUCKET=reasonance_files

# LLM Provider Selection
# Default: use Gemini (cloud). To use Gemini set `LLM_PROVIDER=gemini` and provide `GEMINI_API_KEY` in `.env`.
LLM_PROVIDER=gemini  # Default: Gemini

# Ollama Configuration
OLLAMA_MODEL=gemma3n
OLLAMA_BASE_URL=http://ollama:11434

# Gemini Configuration (if using Gemini)
# Gemini API Configuration (only needed if `LLM_PROVIDER=gemini`)
# be set in `.env` before starting the services when using Gemini.
GEMINI_API_KEY=
GEMINI_MODEL=gemini-3-pro-preview

# Frontend API
BACKEND_API_URL=http://backend:8080/api/v1
```

#### .env.local Overrides (Git-ignored)

Create `.env.local` to override defaults **without committing secrets**:

```env
# Example: Use local backend for development
BACKEND_API_URL=http://localhost:8080/api/v1

# Example: Override database password
POSTGRES_PASSWORD=my-secure-password

# Example: Use Gemini instead of Ollama
LLM_PROVIDER=gemini
GEMINI_API_KEY=sk-xxx...your-api-key
```

**Important**: `.env.local` is git-ignored. Keep production secrets here, not in `.env`.

### Common Tasks

#### Stop Services

```bash
docker-compose down
```

### Stop and Remove Volumes (Clean Start)

```bash
docker-compose down -v
```

#### View Real-time Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f db
```

#### Access Database

```bash
# Connect to PostgreSQL
docker exec -it reasonance-db psql -U reasonance_user -d reasonance_db

# Example queries
\dt                           # List tables
SELECT * FROM spaces;        # View spaces
\q                            # Exit psql
```

#### Access Backend Console

```bash
docker exec -it reasonance-backend bash
```

#### Rebuild Specific Service

```bash
# Rebuild backend only
docker-compose build backend
docker-compose up -d backend

# Rebuild frontend only
docker-compose build frontend
docker-compose up -d frontend
```

### Configuration Reference

#### Database Configuration

Located in backend `application.yaml`, uses environment variables:

* `DB_HOST`: Database hostname (default: `db`)
* `DB_PORT`: Database port (default: `5432`)
* `POSTGRES_DB`: Database name
* `POSTGRES_USER`: Database user
* `POSTGRES_PASSWORD`: Database password

#### RustFS Configuration

* `RUSTFS_URL`: RustFS service URL
* `RUSTFS_BUCKET`: S3 bucket name
* `RUSTFS_ACCESS_KEY`: Access key for RustFS
* `RUSTFS_SECRET_KEY`: Secret key for RustFS

#### LLM Configuration

Default: use Gemini (cloud). To use Gemini set `LLM_PROVIDER=gemini` and provide a valid `GEMINI_API_KEY` in `.env`.

**Option 1: Ollama (Local — optional)**

If you choose Ollama you must run an Ollama server locally and pull the model you intend to use. Example configuration (after pulling `gemma3n` locally):

```env
LLM_PROVIDER=ollama
OLLAMA_BASE_URL=http://ollama:11434
OLLAMA_MODEL=gemma3n
```

**Option 2: Google Gemini (Default)**

Gemini runs in the cloud and requires a valid `GEMINI_API_KEY` in `.env`.

```env
LLM_PROVIDER=gemini
GEMINI_API_KEY=your-api-key-here
GEMINI_MODEL=gemini-3-pro-preview
```

#### Frontend Configuration

* `BACKEND_API_URL`: Backend API endpoint (used at build time)
* Frontend uses relative paths (`/api/v1`) in production (via Nginx proxy)
