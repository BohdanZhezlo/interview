# Bohdan Zhezlo Paidy interview - Forex-mtl service

## Requirements analysis

After reading the initial requirements from the root README file the most obvious solution is to have Forex service as a
simple proxy that forwards all requests to an external provider. However, after investigating OneFrame service
documentation it turned out that it can't support the same load as Forex service is intended to.

Thus, it becomes clear that Forex service should provide some kind of rates caching. It should store rates internally
and use cached items when serving API requests. Beside of that rates should be periodically updated with the frequency
that is less than OneFrame API quota.

Below is the description of possible solutions to how these requirements could be met:

1. _Introduce in-memory cache with expiration in between Forex API and OneFrame API_. With this approach each API call
   tries to fetch the item from the cache. On cache miss it immediately queries OneFrame API and updates the cache. The
   downside of this approach is that on cache miss we introduce a delay in Forex API because it should wait for OneFrame
   API to respond. Beside of that when OneFrame API is down it still tries to call it on every subsequent Forex API call
   causing an avalanche effect (circuit breaker to the rescue).
2. _Introduce async self-refreshing cache_. According
   to [Caffeine documentation](https://github.com/ben-manes/caffeine/wiki/Refresh) it supports async auto refresh. I
   haven't investigated it in details, but it could be a good fit for our requirements. The downside of this approach is
   that this cache is still local so when the application is scaled each node will have its own data, and it'll increase
   the load on OneFrame provider.
3. _Periodic background refresh process_. This approach means that cache refresh is detached into a separate process.
   Forex API just works with abstract rates storage which provides read API for fetching the rate. There is a separate
   background process that queries abstract rate provider service and loads items into the storage. With this
   implementation we could plug in any type of rates storage/cache (in-memory, distributed, distributed with additional
   in-memory caching). Also rates refresh process is detached from the external rates provider so any provide can be
   plugged in.

## Suggested solution

After analyzing requirements and possible implementations I decided to go with option #3 from the list above. In my
implementation I introduced 3 additional services in the application:

- **Provider**. It's an abstraction over 3rd party rates providers. Currently, it supports fetching all the rates at the
  same time. `OneFrameRatesProvider` is a concrete interpreter which is a wrapper around OneFrame. It uses http4s client
  for API calls.
- **Storage**. It's basically a cache for the rates that will be queried by `RatesService`. From the other side there is
  a separate process that periodically updates this storage. In my implementation I'm using in-memory storage with
  Scala `TrieMap` as a cache.
- **Loader**. The purpose of the loader is to periodically poll for rates using provider and store them in storage. It's
  implemented as FS2 stream with periodic awake. Refresh interval is 3 minutes which gives max of 480 requests per day
  which is below OneFrame quota.

Taking this into account we can see that any of these parts can be swapped. For instance, in-memory storage can easily
be swapped with some distributed storage with TTL support (Redis/Memcached/DynamoDB/Cassandra). This allows application
to scale and still meet OneFrame API limitations. With additional improvements, we can have a dedicated role which
responsibility is solely refreshing the storage. And there can be an unlimited number of read-only nodes for serving API
requests.

## Potential improvements / items not done:

1. _Logging and monitoring_. In a real production system this would be a hard requirement. All possible scenarios should
   be logged and metrics reported to external systems.
2. _API parameters validation_. Currently, it silently fails when providing incorrect API params or invalid currency
   pair. API should validate params and clearly report with http response code and message.
3. _Application containerization_.
4. _Handling of expired rates_. Currently, rates are regularly updated with the interval that covers the required max 5
   minutes delay. However, it's possible that the rate provider could fail to respond (e.g. it's hard down for a long
   period of time). In this case application will continue serving outdated rates. Possible improvement is to either
   update in-memory storage with TTL support (e.g. use some in-memory implementation of scalacache) or introduce
   distributed rates storage with TTL support.
5. _Support for read-only/write-only modes_. In current implementation single host is responsible for both periodic
   rates update and serving API requests. It's possible to have dedicated role that runs application in write-only mode
   with disabled HTTP api. The same goes for read-only roles where polling is disabled.
6. _Distributed storage_. As mentioned above any type of distributed storage can be plugged in. Additionally, in
   distributed storage scenario it could still have a local in-memory cache on top of distributed storage to improve
   latency.

## Running the application:

Port has been changed in application config to be 9090. Simply run OneFrame docker container and then run forex-mtl from
the main class. It'll load all rates upon startup so service is immediately ready to respond to API requests:

```
curl "localhost:9090/rates?from=EUR&to=USD" | jq .
```

Response:

```
{
  "from": "EUR",
  "to": "USD",
  "price": 0.6523873889751538,
  "timestamp": "2021-09-01T09:26:44.37Z"
}
```

Re-run the request one more time in 3 min, observe that timestamp on the response has changed.

