# mcapi

A simple mod which provides an HTTP API for users and server owners to interact with Minecraft servers.

**WARNING** This is a work in progress, NOT PRODUCTION READY and not safe to expose to the Internet yet.

## Usage

Use `/mcapi key` in-game to get an apikey. Must be passed as a bearer token with all requests.

The mod provides several APIs to interact with the server.
Some of these are documented in `docs/`.

## Deployment

Add the mod jar to your server's mods folder. There is no need to install on the client.

This will start a server on 127.0.0.1:3333.
Authentication must be currently handled on an external proxy using the reverse proxy auth API - see `ApiServer.java`.
This is not currently implemented in-process but will be optionally in the future.
As a result, you must currently implement this on a proxy in front of `mcapi` such as a cloudflare worker.
A simple example for testing is available in [example-worker.ts](example-worker.ts).

**_MAKE SURE_ to not expose the unauthenticated server publicly.**

**TODO**: Make the host/port configurable, embed token verification into the server optionally.

## Credits

The track watcher code is largely based on [Create: Track Map](https://github.com/jenchanws/create-track-map),
however has been updated and modified to support Create 6.
