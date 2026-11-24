# ІС технічного обслуговування лабораторного обладнання

Курсова робота з дисципліни «Кросплатформене програмування», **варіант №3**.
Виконав: **Бондар Максим Олександрович**, група **203-ТН**. Керівник: Заїка С. О.

Клієнт–серверна система: JavaFX-клієнт → ApiClient → HTTP/JSON → Servlet → Service → DAO → JDBC → MySQL.

## Вимоги до середовища
- JDK 21, Apache Maven 3.9+
- MySQL 8 (або MariaDB 10.6+)
- Apache Tomcat 9.x (javax.servlet 4.0.1; Tomcat 10 не підходить)
- Git

## Структура
```
database.sql                 — створення БД lab_maintenance і демонстраційні дані
server/                      — Maven-проєкт (WAR): model, dao, dao.jdbc, service, servlet
  src/main/webapp/WEB-INF/web.xml
  src/main/resources/db-example.properties
  requests.http              — приклади HTTP-запитів
client/                      — Maven-проєкт JavaFX: model, api (ApiClient), importer (CSV), ui
  sample-data/equipment.csv  — приклад реєстру обладнання для імпорту
```

## 1. База даних
```
mysql -u root -p < database.sql
```
(або phpMyAdmin → вкладка SQL → виконати вміст database.sql). Створюється БД `lab_maintenance` (utf8mb4).

## 2. Сервер
Параметри підключення (пароль у репозиторії не зберігається):
- змінні середовища `DB_URL`, `DB_USER`, `DB_PASSWORD`, **або**
- скопіювати `server/src/main/resources/db-example.properties` у `db.properties` поруч (файл у .gitignore) і вписати пароль.

```
cd server
mvn clean package
```
Скопіювати `target/lab-maintenance-server.war` у `<TOMCAT>/webapps/` і запустити Tomcat (`bin/startup`).
Перевірка: http://localhost:8080/lab-maintenance-server/api/health → `{"status":"UP"}`.
Журнал сервера: `<TOMCAT>/logs/lab-maintenance.log`.

## 3. Клієнт
```
cd client
mvn javafx:run
```
Адреса сервера — `client/src/main/resources/client.properties` або змінна `SERVER_URL`.
Журнал клієнта: `client/client.log`.

## REST API (контекст /lab-maintenance-server)
| Метод | Шлях | Опис |
|---|---|---|
| GET | /api/health | стан сервера |
| GET, POST | /api/equipment | список / створити |
| GET, PUT, DELETE | /api/equipment/{id} | один / змінити / видалити |
| GET, POST | /api/specialists | список / створити |
| GET, PUT, DELETE | /api/specialists/{id} | один / змінити / видалити |
| GET | /api/requests[?status=NEW] | заявки за спаданням I |
| POST | /api/requests | нова заявка (I рахує сервер) |
| GET, PUT, DELETE | /api/requests/{id} | одна / змінити / видалити (лише «Нова») |
| POST | /api/requests/{id}/status | перехід за життєвим циклом |
| GET, POST | /api/requests/{id}/records | сервісні записи |
| PUT, DELETE | /api/requests/{id}/records/{rid} | змінити / видалити запис |

Помилки: 400 — некоректні дані, 404 — не знайдено, 409 — конфлікт стану, 500 — збій сервера.

## Життєвий цикл заявки
НОВА → ДІАГНОСТОВАНО (фахівець + діагноз) → ОБСЛУГОВУВАННЯ → ВИКОНАНО (≥ 1 сервісний запис).

## Інтегральний показник
I = 0,40·F1 + 0,35·F2 + 0,25·F3, де
F1 = (критичність − 1)/4·100, F2 = (тяжкість − 1)/4·100, F3 = min(простій, 72)/72·100.

## Формат CSV (реєстр обладнання)
UTF-8, роздільник `;`, перший рядок — заголовок:
```
inventory_number;name;model;location;criticality;commissioned_date
INV-0101;Осцилограф цифровий;Rigol DS1202Z-E;ауд. 312;4;2024-09-01
```
Обов'язкові: inventory_number, name, criticality (1–5); дата — РРРР-ММ-ДД або порожньо.
Некоректні рядки та дублікати інвентарних номерів пропускаються, підсумок показується після імпорту.

## Тести
```
cd server && mvn clean test     # 31 тест
cd client && mvn clean test     # 7 тестів
```

## Сценарій демонстрації
1. Запустити MySQL і Tomcat, перевірити /api/health.
2. `mvn javafx:run` у client — у рядку стану «підключено».
3. Обладнання: додати, змінити, видалити запис; «Імпорт CSV…» → sample-data/equipment.csv (12 імпортовано, 4 пропущено).
4. Фахівці: додати/змінити/видалити.
5. Заявки: створити заявку (показується I), пройти Нова → Діагностовано → Обслуговування → (додати запис) → Виконано.
6. Показати журнал `lab-maintenance.log` / `client.log`.
7. `mvn clean test`.
