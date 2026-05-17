# File Task Queue Service

Асинхронный сервис для обработки задач на обработку файлов с поддержкой очередей и управления статусами.

## Используемые технологии

| Компонент             | Технология                                   |
|-----------------------|----------------------------------------------|
| Язык программирования | Kotlin 2.1.10                                |
| Фреймворк             | Spring Boot 3.2.5                            |
| ORM                   | Spring Data JPA (Hibernate)                  |
| База данных           | PostgreSQL (H2 для тестирования)             |
| Миграции              | Liquibase                                    |
| Документация API      | SpringDoc OpenAPI (Swagger)                  |
| Сборка                | Gradle 8.10                                  |
| Тестирование          | JUnit 5, MockK, MockMVC, Testcontainers      |
| Мониторинг            | Spring Boot Actuator, Micrometer, Prometheus |
| Контейнеризация       | Docker & Docker Compose                      |

## Функциональные требования
- число одновременно выполняемых задач должно задаваться в конфиге
- задача должна переходить по статусам корректно
- ошибки обработки должны сохраняться
- должны быть фильтры по статусу, типу задачи, времени создания
- должна быть пагинация
- задачи должны обрабатываться асинхронно
- дополнительно:
  - retry для упавших задач
  - scheduler для поднятия “зависших” задач
  - историю смены статусов
  - приоритет задач
  - отдельную таблицу событий по задаче
  - метрики по времени выполнения

## Сборка и запуск

### Требования

- JDK 21 или выше
- Gradle 8.10
- Docker и Docker Compose (для запуска PostgreSQL и самого приложения)

### Запуск с Docker Compose

```bash
docker compose down
docker compose up --build -d
```

Приложение будет доступно по адресу: http://localhost:8080

## API

Swagger UI: http://localhost:8080/swagger-ui.html

### Основные эндпоинты

- Создание задачи:

```bash
POST /api/v1/tasks
Content-Type: application/json

{
  "fileName": "document.pdf",
  "filePath": "/uploads/document.pdf",
  "fileSize": 1024000,
  "mimeType": "application/pdf",
  "type": "DOCUMENT_PARSING",
  "priority": 5
}
```

- Получение задачи по ID:

```bash
GET /api/v1/tasks/{id}
```

- Получение списка задач с фильтрами:

```bash
GET /api/v1/tasks?status=PENDING&type=IMAGE_PROCESSING&page=0&size=20
```

- Отмена задачи:

```bash
DELETE /api/v1/tasks/{id}
```

- Запуск обработки:

```bash
POST /api/v1/tasks/start-processing
```

- Остановка обработки:

```bash
POST /api/v1/tasks/stop-processing
```

- Статус обработки:

```bash
GET /api/v1/tasks/status
```

## Конфигурация

Основные параметры в `application.yml`:

```yaml
task-queue:
  max-concurrent-tasks: 5              # Максимальное количество одновременно выполняемых задач
  retry:
    enabled: true                      # Включить/выключить автоматический retry
    max-attempts: 3                    # Максимальное количество попыток выполнения
    backoff-delay-ms: 5000             # Задержка между попытками (миллисекунды)
  scheduler:
    enabled: true                      # Включить/выключить проверку зависших задач
    stuck-task-timeout-minutes: 30     # Таймаут для определения "зависшей" задачи
    check-interval-ms: 60000           # Интервал проверки зависших задач
```

### Типы задач

- IMAGE_PROCESSING - обработка изображений
- VIDEO_CONVERSION - конвертация видео
- DOCUMENT_PARSING - парсинг документов
- ARCHIVE_EXTRACTION - распаковка архивов
- DATA_IMPORT - импорт данных
- CUSTOM - пользовательская обработка

### Статусы задач

- PENDING - ожидает обработки
- PROCESSING - в процессе обработки
- COMPLETED - успешно завершена
- FAILED - завершена с ошибкой
- CANCELLED - отменена пользователем

## Тестирование

### Запуск всех тестов

```bash
./gradlew test
```