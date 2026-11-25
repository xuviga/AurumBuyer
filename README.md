# AurumBuyer

[![build](https://github.com/xuviga/AurumBuyer/actions/workflows/build.yml/badge.svg)](https://github.com/xuviga/AurumBuyer/actions/workflows/build.yml)
[GitHub](https://github.com/xuviga/AurumBuyer)

**Автор:** XuViGaN
**Версия:** 1.2.0
**Платформа:** Paper / Spigot 1.16.1 – 1.21.x и 26.x (один jar, байткод Java 8)
**Зависимости:** нет. Vault и PlaceholderAPI — опциональны.

AurumBuyer — продвинутый скупщик предметов: продажа за деньги и аурумы, множитель,
бустеры категорий, «ульта», лимиты по предметам, магазин, статистика и плейсхолдеры.

---

## Установка

1. Положите `AurumBuyer-1.2.0.jar` в `plugins/`.
2. Запустите сервер — создастся папка `plugins/AurumBuyer/` с конфигами.
3. (Опционально) установите Vault + любой плагин экономики, чтобы деньги шли в общую экономику.
   Без Vault плагин работает в режиме `INTERNAL` (собственный баланс в `playerdata`).
4. (Опционально) установите PlaceholderAPI для плейсхолдеров `%aurum_...%`.

### Файлы

```
plugins/AurumBuyer/
  config.yml          настройки ядра, GUI, экономики
  messages.yml        все сообщения (только русский текст здесь)
  prices.yml          цены скупки + wildcard-группы
  shop.yml            товары магазина
  boosters.yml        категории бустеров
  globallimits.yml    глобальные лимиты (создаётся автоматически)
  playerdata/<uuid>.yml  профили игроков
```

---

## Команды

| Команда | Права | Описание |
|---|---|---|
| `/aurum` | `aurum.use` | главное меню |
| `/aurum help` | `aurum.use` | список команд |
| `/aurum sell` | `aurum.sell` | продать весь инвентарь |
| `/aurum auto [on\|untilquit\|off]` | `aurum.auto` | автопродажа |
| `/aurum shop [page]` | `aurum.shop` | магазин на аурумы |
| `/aurum boosters` | `aurum.boosters` | бустеры и ульта |
| `/aurum history [page]` | `aurum.use` | история продаж |
| `/aurum limits` | `aurum.use` | лимиты |
| `/aurum settings` | `aurum.use` | настройки |
| `/aurum top [money\|points]` | `aurum.top` | топ игроков в чат |
| `/aurum reload` | `aurum.admin` | перезагрузить конфиги |
| `/aurum admin ...` | `aurum.admin` | администрирование |

Алиасы: `ab`, `aurumbuyer`, `аурум`.

### `/aurum admin`

```
admin reload
admin save
admin points give|take <игрок> <кол-во>
admin ulta   give|take <игрок> <кол-во>
admin level  set <игрок> <категория> <уровень>
admin limit  reset <игрок>|global
admin reset  <игрок>
```

---

## Права

| Право | По умолчанию |
|---|---|
| `aurum.use` | true |
| `aurum.sell` | true |
| `aurum.shop` | true |
| `aurum.auto` | true |
| `aurum.boosters` | true |
| `aurum.top` | true |
| `aurum.admin` | op |

---

## Плейсхолдеры

Идентификатор расширения — `aurum`. Работают и внутри `messages.yml` / лора GUI.
```
%aurum_points%  %aurum_total_points%  %aurum_total_money%  %aurum_balance%
%aurum_ulta%  %aurum_ulta_max%
%aurum_multiplier%  %aurum_multiplier_percent%
%aurum_auto_mode%
%aurum_sold_<ITEM>%
%aurum_top_money_<n>%  %aurum_top_money_<n>_name%  %aurum_top_money_<n>_amount%
%aurum_top_points_<n>% %aurum_top_points_<n>_name% %aurum_top_points_<n>_amount%
%aurum_booster_<cat>_cost%         %aurum_booster_<cat>_cost_<level>%
%aurum_booster_<cat>_percent%      %aurum_booster_<cat>_percent_<level>%
%aurum_booster_<cat>_next_bonus%   %aurum_booster_<cat>_next_percent%
%aurum_booster_<cat>_multiplier%   %aurum_booster_<cat>_level%
%aurum_booster_<cat>_max_level%    %aurum_booster_<cat>_is_max%
%aurum_booster_<cat>_spent%        %aurum_booster_spent_total%
```

Неизвестный идентификатор даёт пустую строку. `%aurum_top_money_<n>%` без
суффикса возвращает **имя** игрока; сумма — `_amount`, отдельно имя — `_name`.

---

## Механика

### Продажа
`unit = price × множитель_категории × множитель_скупщика × лимитный_множитель`,
`money = unit × количество`,
`аурумы = money × sell.points-percent%`,
`ульта = money × ulta.points-percent%`.

`множитель_категории = 1 + (постоянный_бонус_бустера + временный_бонус_ульты) / 100`;
категория предмета берётся из `prices.yml → category`.

По умолчанию слот `0` хотбара не продаётся (`auto-sell.ignore-slots`) — там обычно
лежит основной инструмент. Список игнорируемых слотов настраивается.

### Множитель скупщика
Растёт от накопленных продаж (`moneyTotal`). При непустом `multiplier.tiers`
процент = `per-step-percent × индекс_достигнутого_порога`, иначе
`floor(moneyTotal / step) × per-step-percent`, с ограничением `max-percent`.

### Лимиты
Для предметов с `limited.global` / `limited.personal`. Глобальные хранятся в
`globallimits.yml` и сбрасываются каждые `limits.global-reset-hours`; личные — в
профиле игрока, период `limits.personal-reset-hours`. Лимит — это премиальная
квота: пока остаток не исчерпан, предмет продаётся с `multiplier` из `prices.yml`
(цены выше обычных), а сверх квоты продажа не идёт — предметы остаются в
инвентаре, игроку приходит уведомление `sell-limit-hit`.

У wildcard-групп (`groups.*`) счётчик общий для всей группы: продажа
`OAK_LOG`/`BIRCH_LOG` расходует один лимит `*_LOG`.

`prices.yml → model-data`: если у записи задан custom model data, по этой цене
продаётся только предмет с совпадающим CMD, остальные предметы того же
материала считаются непродаваемыми по этой записи.

### Бустеры
`cost(level) = base-cost × (1 + cost-growth-percent/100)^level`, бонус категории
`percent-per-level × level`. Покупаются за аурумы.

### Ульта
Копится с продаж, активируется за `ulta.cost` аурумов-очков, даёт временный бонус
`ulta.percent` на все категории (включая категории без постоянного бустера) на
`ulta.minutes` минут с откатом `ulta.cooldown-minutes`. Временные бонусы
сохраняются между перезапусками.

### Магазин
`cost = base-cost × (1 + price-increase-percent/100)^куплено_игроком`, оплата аурумами,
поддержка личных и глобальных лимитов, команды выдачи после покупки.
Если товаров больше, чем слотов на странице, магазин листается кнопками
«Назад/Вперёд»; `Shift+ЛКМ` покупает до `shop.max-purchases-per-click` штук за раз.

---

## Конфигурация (кратко)

`config.yml → economy.mode`: `VAULT` (деньги через Vault) или `INTERNAL`
(собственный баланс плагина — работает без зависимостей). Режим и
`currency-format` перечитываются по `/aurum reload`.
Отображаемое имя валюты задаётся в `messages.yml → points-name`.
`config.yml → gui`: материалы, слоты, названия и `custom-model-data` всех кнопок.

Все пользовательские тексты — в `messages.yml`; токен `{points}` заменяется на
`points-name` (по умолчанию `&6Аурумы`).

---

## Поддерживаемые версии

| Платформа | Версии | Примечание |
|---|---|---|
| Paper / Spigot | 1.16.1 – 1.21.x | профиль по умолчанию (`spigot-api 1.16.5`) |
| Paper | 26.x | профиль `latest` (`spigot-api 26.3`) |

Один jar на все версии (байткод Java 8). Проверено на **Paper 26.3 build 262 / Java 25**
(профиль `latest`).

## Сборка

```bash
mvn -B clean package
# target/AurumBuyer-1.2.0.jar
mvn -B test          # юнит-тесты формул и профиля

# сборка против новейшего API (Paper 26.x)
mvn -B -Platest -DskipTests compile
```

Требуется JDK 8+ (сборка проверена на JDK 21 с `release 8`, профиль `latest` —
на JDK 25).

---

## История изменений

### 1.2.0
- официальная поддержка Paper 26.x: профиль сборки `latest` (`spigot-api 26.3`);
- CI-матрица: JDK 21 (legacy verify) и JDK 25 (`-Platest`);
- проверено на Paper 26.3 build 262 / Java 25; версия 1.2.0.

### 1.1.0
- бустеры и «ульта» теперь реально участвуют в цене продажи;
- авто-продажа работает в режиме `untilquit` и проверяет `min-items` до продажи;
- добавлены обработчики входа/выхода: имя обновляется, профиль выгружается, `untilquit` сбрасывается;
- лимиты wildcard-групп считаются по группе; `globallimits.yml` не пишется на каждый стак;
- фильтр `model-data` в `prices.yml` действительно фильтрует предметы;
- смена `economy.mode` применяется по `/aurum reload`;
- предметы удаляются по реальным слотам инвентаря;
- магазин: пагинация, `Shift+ЛКМ`, сообщения о лимите, рабочий `shop.require-permission`;
- история продаж хранит фактическую сумму по каждому предмету;
- при полном исчерпании квоты показывается сообщение о лимите, а не «нечего продавать»;
- плейсхолдеры `%aurum_*%` разворачиваются в чате и лорах меню;
- удалён мёртвый код; добавлены юнит-тесты; версия 1.1.0.
