# Микросервис Банковских Операций и Лимитов

Этот проект представляет собой Spring Boot микросервис, предназначенный для:
*   Регистрации расходных банковских транзакций.
*   Хранения и получения актуальных курсов валют (с использованием внешнего API Twelve Data).
*   Управления месячными лимитами расходов по категориям (товары/услуги).
*   Отслеживания и получения списка транзакций, превысивших установленный лимит.

## Технологии

*   Java 17+
*   Spring Boot 3+
*   Spring Data JPA (для работы с PostgreSQL)
*   Spring Data Cassandra (для работы с Apache Cassandra)
*   Spring Web (REST API)
*   Spring WebFlux (WebClient для внешнего API)
*   PostgreSQL (хранение транзакций и лимитов)
*   Apache Cassandra (хранение курсов валют)
*   Liquibase (управление миграциями схемы PostgreSQL)
*   MapStruct (маппинг между DTO и Entity)
*   Lombok
*   Maven (сборка проекта)
*   Docker & Docker Compose (запуск окружения)
*   Springdoc OpenAPI (документация API / Swagger UI)

## Функционал

*   **Прием транзакций:** Эндпоинт для получения данных о транзакции (счет отправителя/получателя, сумма, валюта, категория).
*   **Конвертация в USD:** Автоматическая конвертация суммы транзакции в USD по курсу на день транзакции (или последнему доступному). Поддерживается расчет кросс-курса KZT/USD через KZT/RUB и RUB/USD.
*   **Проверка лимитов:** Сравнение суммы транзакции (в USD) с месячным лимитом, установленным для ее категории. Установка флага `limitExceeded`.
*   **Установка лимитов:** Эндпоинт для установки нового месячного лимита для категории (в USD).
*   **Получение курсов валют:** Автоматическая загрузка курсов с внешнего API (Twelve Data) по расписанию и при старте.
*   **API для клиента:**
    *   Получение списка транзакций, превысивших лимит.
    *   Получение списка актуальных курсов валют на сегодня.
    *   Установка нового лимита.

## Подготовка к запуску

### 1. Клонирование репозитория

Склонируйте репозиторий на ваш локальный компьютер:
```bash
git clone https://github.com/abarigena/Task-Bank-Operation.git
cd Task-Bank-Operation/Bank-Operation
```
Все последующие команды (Maven, Docker Compose) должны выполняться из папки `./Bank-Operation`.

### 2. Переменные окружения

Для работы приложения необходимо создать файл `.env` в папке `./Bank-Operation`. Этот файл будет использоваться Docker Compose для передачи секретных данных в контейнер приложения.

**Создайте файл `./Bank-Operation/.env` со следующим содержимым:**

```dotenv
# Пароль для пользователя postgres в базе данных PostgreSQL
# (этот пароль будет использован и для создания базы в Docker Compose)
DB_PASSWORD=your_strong_postgres_password

# Имя пользователя для PostgreSQL (обычно 'postgres')
DB_USERNAME=postgres

# Ваш API ключ для сервиса twelvedata.com
API_KEY=your_twelvedata_api_key
```

**Замените `your_strong_postgres_password` и `your_twelvedata_api_key` на ваши реальные значения.**

### 3. Конфигурация валютных пар

Список валютных пар, курсы которых загружаются из внешнего API, настраивается в файле `src/main/resources/application.yml` в секции `app.exchange.currencies`.

Текущая конфигурация (пример):
```yaml
app:
  exchange:
    # Список валютных пар для загрузки курсов через Twelve Data
    currencies: EUR/USD,RUB/USD,KZT/RUB
```
Вы можете изменить этот список, указав другие пары, поддерживаемые API Twelve Data, через запятую. Для корректного расчета кросс-курса KZT/USD необходимо наличие пар `KZT/RUB` и `RUB/USD` в этом списке.

## Сборка и Запуск

Для запуска приложения и необходимых баз данных (PostgreSQL, Cassandra) используется Docker Compose. Все команды выполняются из папки `./Bank-Operation`.

1.  **Сборка проекта:** Убедитесь, что у вас установлен Maven и JDK 17+. Выполните команду:
    ```bash
    mvn clean package
    ```

2.  **Запуск Docker Compose:** Убедитесь, что у вас установлен Docker и Docker Compose. Выполните команду:
    ```bash
    docker compose up --build
    ```

3.  **Остановка:** Чтобы остановить все контейнеры, нажмите `Ctrl+C` в терминале, где запущен `docker compose up`, или выполните в другом терминале (из папки `./Bank-Operation`):
    ```bash
    docker compose down
    ```

Приложение будет доступно по адресу `http://localhost:8000` (или на порту, указанном в `server.port`).

## Документация API (Swagger UI)

Интерактивная документация API доступна после запуска приложения по адресу:

[http://localhost:8000/swagger-ui.html](http://localhost:8000/swagger-ui.html)

Swagger UI позволяет просматривать все доступные эндпоинты, их описания, ожидаемые параметры запросов, модели данных (DTO). Вы также можете отправлять тестовые запросы прямо из интерфейса Swagger UI.

## Примеры запросов API

### 1. Регистрация новой транзакции

*   **Метод:** `POST`
*   **URL:** `/api/transactions`
*   **Тело запроса (Request Body):** `application/json`

```json
{
  "account_from": "1000000001",
  "account_to": "9999999999",
  "currency_shortname": "KZT",
  "sum": 150000.50,
  "expense_category": "SERVICE",
  "datetime": "2025-03-29T12:00:00Z" 
}
```

### 2. Установка нового лимита

*   **Метод:** `POST`
*   **URL:** `/api/limits`
*   **Тело запроса (Request Body):** `application/json`

```json
{
  "limitSum": 2500.00,
  "expenseCategory": "PRODUCT"
}
```

### 3. Получение транзакций, превысивших лимит

*   **Метод:** `GET`
*   **URL:** `/api/transactions/exceeded`

### 4. Получение курсов валют на сегодня

*   **Метод:** `GET`
*   **URL:** `/api/rates/today`

