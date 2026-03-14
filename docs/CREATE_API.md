# Create Railways API Documentation

## Overview

The Create Railways API provides endpoints for managing railway stations and trains within the Create mod ecosystem.

---

## Authentication

You have to authenticate in order to use the API via an API key.

This key can be obtained in game using the `/mcapi key` command.
Calling the command will invalidate your previous key and generate a new one.
_It will only be shown once, so make sure to click to copy it and save it somewhere._

It is equivalent to a password, so **do not** commit it to Git (use `.env` files instead) or otherwise publish it on the Internet.
If it has been committed to Git or published on the internet, regenerate it immediately using the `/mcapi key` command.

The API key must be sent along with all requests in the `Authorization` header as follows:

```
Authorization: Bearer <apikey>
```

For trains, you will only be able to request and modify trains which are owned by your user.
You can take ownership of a train in-game by disassembling and reassembling it at a station.

---

## Endpoints

### Stations

#### Get All Stations

Returns a list of all railway stations across all track networks.

```
GET /createmod/stations
```

**Response:** `200 OK`

```json
{
  "station-uuid-1": {
    "name": "Central Station",
    "graph": "network-uuid-1"
  },
  "station-uuid-2": {
    "name": "North Terminal",
    "graph": "network-uuid-2"
  }
}
```

**Response Fields:**

| Field         | Type   | Description                                       |
|---------------|--------|---------------------------------------------------|
| `{stationId}` | Object | Station object keyed by station UUID              |
| `name`        | String | Display name of the station                       |
| `graph`       | String | UUID of the track network this station belongs to |

**Error Responses:**

| Status                      | Response                     |
|-----------------------------|------------------------------|
| `500 Internal Server Error` | `{"error": "error message"}` |

---

### Trains

#### Get All Trains

Returns all trains accessible to the authenticated user.

```
GET /createmod/train
```

**Response:** `200 OK`

```json5
{
  "train-uuid-1": {
    "name": "Express A",
    "owner": "user-uuid-1",
    "graph": "network-uuid-1",
    "derailed": false,
    "currentlyBackwards": false,
    "runtime": {
      "schedule": {
        /* NBT data */
      },
      "completed": 5,
      "currentEntry": 2,
      "currentTitle": "The current schedule title (label)",
      "paused": false,
      "ticksInTransit": 1200,
      "isAutoSchedule": false,
      "state": "IN_TRANSIT"
    }
  }
}
```

**Response Fields:**

| Field                    | Type          | Description                                       |
|--------------------------|---------------|---------------------------------------------------|
| `{trainId}`              | Object        | Train object keyed by train UUID                  |
| `name`                   | String        | Display name of the train                         |
| `owner`                  | String (UUID) | UUID of the train owner                           |
| `graph`                  | String (UUID) | UUID of the track network this train is on        |
| `derailed`               | Boolean       | Whether the train is currently derailed           |
| `currentlyBackwards`     | Boolean       | Whether the train is moving backwards             |
| `runtime.schedule`       | Object        | NBT-encoded schedule data (null if no schedule)   |
| `runtime.completed`      | Integer       | Number of completed schedule entries              |
| `runtime.currentEntry`   | Integer       | Index of current schedule entry                   |
| `runtime.currentTitle`   | String        | Human-readable description of current destination |
| `runtime.paused`         | Boolean       | Whether the train is paused                       |
| `runtime.ticksInTransit` | Integer       | Game ticks spent in transit                       |
| `runtime.isAutoSchedule` | Boolean       | Whether using automatic scheduling                |
| `runtime.state`          | String        | Current train state                               |

**Error Responses:**

| Status                      | Response                     |
|-----------------------------|------------------------------|
| `500 Internal Server Error` | `{"error": "error message"}` |

---

#### Update Multiple Trains

Updates runtime properties for multiple trains in a single request.

```
POST /createmod/train
Content-Type: application/json
```

**Request Body:**

```json5
{
  "train-uuid-1": {
    "runtime": {
      "paused": true,
      "currentEntry": 3,
      "isAutoSchedule": false,
      "schedule": {
        /* NBT data */
      }
    }
  },
  "train-uuid-2": {
    "runtime": {
      "paused": false
    }
  }
}
```

**Request Fields:**

| Field                    | Type    | Description                                 |
|--------------------------|---------|---------------------------------------------|
| `{trainId}`              | Object  | Train UUID as key                           |
| `runtime`                | Object  | Runtime properties to update (all optional) |
| `runtime.schedule`       | Object  | NBT-encoded schedule data (null to clear)   |
| `runtime.paused`         | Boolean | Pause/resume the train                      |
| `runtime.currentEntry`   | Integer | Set the current schedule entry              |
| `runtime.isAutoSchedule` | Boolean | Enable/disable automatic scheduling         |

**Response:** `204 No Content` (on success)

**Error Responses:**

| Status                      | Response                                             |
|-----------------------------|------------------------------------------------------|
| `400 Bad Request`           | `{"error": "Content-Type must be application/json"}` |
| `400 Bad Request`           | `{"error": "Root node must be object"}`              |
| `400 Bad Request`           | `{"error": "error message"}` (validation error)      |
| `500 Internal Server Error` | `{"error": "error message"}`                         |

---

#### Get Train by ID

Returns a specific train by UUID.

```
GET /createmod/train/{id}
```

**Path Parameters:**

| Parameter | Type | Description    |
|-----------|------|----------------|
| `id`      | UUID | The train UUID |

**Response:** `200 OK`

```json5
{
  "name": "Express A",
  "owner": "user-uuid-1",
  "graph": "network-uuid-1",
  "derailed": false,
  "currentlyBackwards": false,
  "runtime": {
    "schedule": {
      /* NBT data */
    },
    "completed": 5,
    "currentEntry": 2,
    "currentTitle": "The current schedule title (label)",
    "paused": false,
    "ticksInTransit": 1200,
    "isAutoSchedule": false,
    "state": "IN_TRANSIT"
  }
}
```

**Error Responses:**

| Status                      | Response                                      |
|-----------------------------|-----------------------------------------------|
| `400 Bad Request`           | `{"error": "Train UUID is not valid"}`        |
| `404 Not Found`             | `{"error": "Train with this uuid not found"}` |
| `500 Internal Server Error` | `{"error": "error message"}`                  |

---

#### Update Train by ID

Updates runtime properties for a specific train.

```
POST /createmod/train/{id}
Content-Type: application/json
```

**Path Parameters:**

| Parameter | Type | Description    |
|-----------|------|----------------|
| `id`      | UUID | The train UUID |

**Request Body:**

```json5
{
  "runtime": {
    "paused": true,
    "currentEntry": 3,
    "isAutoSchedule": false,
    "schedule": {
      /* NBT data */
    }
  }
}
```

**Request Fields:**

| Field                    | Type    | Description                                 |
|--------------------------|---------|---------------------------------------------|
| `runtime`                | Object  | Runtime properties to update (all optional) |
| `runtime.schedule`       | Object  | NBT-encoded schedule data (null to clear)   |
| `runtime.paused`         | Boolean | Pause/resume the train                      |
| `runtime.currentEntry`   | Integer | Set the current schedule entry              |
| `runtime.isAutoSchedule` | Boolean | Enable/disable automatic scheduling         |

**Response:** `204 No Content` (on success)

**Error Responses:**

| Status                      | Response                                             |
|-----------------------------|------------------------------------------------------|
| `400 Bad Request`           | `{"error": "Content-Type must be application/json"}` |
| `400 Bad Request`           | `{"error": "Train UUID is not valid"}`               |
| `400 Bad Request`           | `{"error": "error message"}` (validation error)      |
| `404 Not Found`             | `{"error": "Train with given uuid not found"}`       |
| `500 Internal Server Error` | `{"error": "error message"}`                         |

---

### Utilities

#### Get Train by Name

Returns the UUID(s) of all trains with a specific name accessible to the authenticated user.

```
GET /createmod/train/by-name/{name}
```

**Path Parameters:**

| Parameter | Type   | Description                        |
|-----------|--------|------------------------------------|
| `name`    | String | The exact train name to search for |

**Response:** `200 OK`

```json
[
  "train-uuid-1",
  "train-uuid-2"
]
```

**Error Responses:**

| Status                      | Response                     |
|-----------------------------|------------------------------|
| `500 Internal Server Error` | `{"error": "error message"}` |

---

## Notes

### Error Handling

All endpoints include comprehensive error handling with appropriate HTTP status codes and JSON error messages.

**Error JSON Format**:

```json
{
  "error": "<message>"
}
```
