// A simple Cloudflare worker to serve mcapi's create mod API over the Internet using Cloudflare Workers KV and Workers VPC.

export default {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  async fetch(request, env, _ctx): Promise<Response> {
    // Get the Authorization header
    const authHeader = request.headers.get('Authorization');
    const token = authHeader?.slice(7);

    try {
      if (!authHeader || !token || !authHeader.startsWith('Bearer ')) {
        throw new Error('Missing or invalid authorization header');
      }

      // Decode from base64url
      const decoded = Buffer.from(token, 'base64url');

      // Hash with SHA-256
      const hashBuffer = await crypto.subtle.digest('SHA-256', decoded);

      // Encode hash to base64url
      const hashBase64 = Buffer.from(hashBuffer).toString('base64url');

      // check KV cache, if it is cached use that
      let uuid: string;
      const kvKeyName = `keyhash:${hashBase64}`;
      const kvKey = await env.KV.get(kvKeyName);
      if (kvKey) {
        console.log('has KV');
        uuid = kvKey;
      } else {
        console.log('does not have KV');
        // Fetch from remote /auth/{hash} endpoint
        const response = await env.VPC.fetch(`http://localhost/auth/${hashBase64}`);

        if (!response.ok) {
          let error: string | null = null;
          try {
            const errData: { error: string } = await response.json();
            if (errData['error']) {
              error = errData.error;
            }
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
          } catch (_) {
            // do nothing
          }
          throw Error(
            error ? `auth API failed with error ${error}` : 'auth API response was not ok'
          );
        }

        const userId: { uuid: string } = await response.json();
        if (!userId['uuid']) {
          throw Error('auth API response did not have uuid field');
        }

        uuid = userId.uuid;

        // store with short TTL (1m) in KV
        await env.KV.put(kvKeyName, uuid, { expirationTtl: 60 });
      }

      const url = new URL(request.url);
      const pathname = url.pathname;

      return env.VPC.fetch(`http://localhost/createmod${pathname}`, {
        headers: { 'X-Mcapi-Uuid': uuid }
      });
    } catch (error) {
      console.error('Validation error:', error);
      return new Response(JSON.stringify({ error: 'Authentication failed' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' }
      });
    }
  }
} satisfies ExportedHandler<Env>;
