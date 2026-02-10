# I Tried Request Batching + DOD in a Backend API (And It Actually Changed Things)

> Draft template for Medium.  
> Note: Numbers are placeholders for now and should be replaced after final performance runs.

---

## Expectation: Why this even matters

I have always felt backend performance work is one of those things that sounds "nice to have" until you see what it does at scale.

For one API, saving a few milliseconds might not look dramatic. But when the same pattern exists across many services in an org, those wins stack up. Fewer servers, better throughput, lower p95 latency, less noise during peak traffic. Even with hardware being cheaper than before, performance improvements can still save serious money at the organization level.

I wanted to see if ideas from a completely different world—the one that pushes hardware to its limits to render high-quality, high-performance games—could apply to backend APIs. So I read up, tried a few things, and this article is about what I did and what I learned.

My expectation going in was simple:

- increase throughput under load
- reduce average and tail latency
- reduce per-request overhead by doing work in batches

[Optional sentence to customize with your own thought]:  
"I did not expect magic, but I expected enough improvement to justify the added complexity."

---

## Motivation: A paradigm from gaming, and why I tried it in backend

Modern games do something that still feels like magic when you think about it: they render huge, detailed worlds at 60 or 120 frames per second, with thousands of entities—characters, particles, physics objects—updating every frame. That doesn’t happen by accident. The gaming industry relies on a way of thinking about code and data that most backend folks never hear about. It’s called **Data-Oriented Design**, or **DOD**.

I’d never run into it in my day job. Backend talks are full of APIs, databases, and caches—and when we say “cache,” we usually mean Redis or something like it. In the DOD world, “cache” usually means the **CPU cache**: the small, extremely fast memory built into the processor for both data and code. DOD is about designing for that hardware—how you lay out and access data, and how your code is structured—so the machine can do the work efficiently.

I started reading about DOD and one idea stuck: in hot paths, *how* data is laid out and *how* you walk it can matter as much as what you compute. For backend APIs, on a high level, database queries are often the heavy operations. To optimize, we can reduce the number of queries we fire by batching incoming requests over a short time window and processing them together. This technique is also known as **request coalescing**. The two fit nicely:

- **DOD mindset:** Organize data and access it in a way that’s friendly to the CPU cache—sequential, predictable, less jumping around.
- **Request coalescing:** Do the work once per batch instead of once per request.

I decided to try both in a small backend API and see what happened. This article is not “I found the perfect architecture.” It’s “here’s what I tried and what I learned.”

### What is DOD? (Spelled out for backend readers)

If you’ve never heard of Data-Oriented Design, you’re in good company. Most backend codebases don’t talk about it. The idea comes from game development and other high-performance domains.

**In short:** DOD is about designing for the hardware your code runs on. You make choices—how data is laid out, how you access it, how your code is structured—so that the machine can do the work efficiently. In that sense, “data” in DOD means both the *values* you process and the *code* you run. We care about the **data cache** (the CPU’s cache for the values in memory) and the **code cache** (its cache for the instructions it’s executing) just as much. Good layout and access patterns help the data cache; tight, linear code paths and less branching help the code cache. (When I say “cache” here, I mean these CPU caches, not Redis or a distributed cache.)

**What “pointer chasing” looks like (and how DOD improves it):** In backend code we often have an entity with navigation properties—e.g. a `Comic` that has `Chapters`, and each chapter has a `ReleaseDate`. To compute something over all chapters, you write a loop that touches `comic.Chapters`, then each `chapter.ReleaseDate`. Each of those objects can live in a different place in memory. So the CPU follows a reference to the comic, then to the list of chapters, then to chapter 1, then to chapter 2, and so on. Every hop can mean a data-cache miss—the CPU has to wait for RAM. That’s pointer chasing: the CPU is constantly chasing references from one object to the next.  
DOD-style improvement: instead of walking that graph, you first copy or project the data you need into a simple, contiguous layout—e.g. one array of release dates for all the chapters you’re about to process. Then your hot loop just iterates over that array in order. The CPU reads memory sequentially, the data cache stays hot, and you do a lot less chasing. Same work, less jumping around.

**Where it shows up:** It’s big in game engines and real-time graphics. When you have to update thousands of entities every frame (physics, AI, rendering), the cost of following pointers and jumping around memory adds up fast. Games need to hit 60 or 120 fps, so engine programmers have been forced to think in terms of data layout and how the CPU cache is used. You’ll see DOD-style ideas in talks and code from Unity, Unreal, and other engines—and in a lot of “why is my game slow?” deep dives.

**Why I was curious:** That kind of impact in gaming made me wonder what happens when you borrow the same mindset for backend services. We’re not rendering frames, but we do have hot paths that touch a lot of data per request. If organizing that data and processing it in batches can squeeze out more throughput and lower latency, it felt worth trying.

---

## Setup: Comic visibility scenario (fabricated but practical)

To test this, I used a fabricated backend scenario that is still close to real production patterns.

Imagine an API that computes comic book visibility based on:

- geography rules
- customer segment rules
- release timing
- pricing and other content flags

Each request asks for visibility of multiple comics for a given user context. During load, many requests come in around the same time.

Instead of computing each request independently, I batched requests over a short time window and processed the batch together.

### Baseline (before batching)

- one request processed at a time
- repeated lookups / repeated work per request
- object-graph heavy traversal during compute

[Add your exact baseline details later]

### Batched approach (after)

- coalesce multiple incoming requests
- process all collected comic IDs together
- compute visibilities in a single batch pipeline
- map results back to original requests

[Add exact implementation details from your code later]

### Why I expected improvement

The biggest expected gain was not one tiny micro-optimization. It was from changing the *shape of work*:

- fewer repeated operations across requests
- better locality when processing related data together
- less per-request overhead in the hot path

---

## Load test results (placeholder section)

I will finalize this section once I complete the final performance runs.

For now, here is the structure I plan to use.

### Test environment

- machine/spec: `[TODO]`
- dataset size: `[TODO]`
- concurrency levels: `[TODO]`
- test duration and warmup: `[TODO]`
- tooling used: `[TODO]`

### Metrics captured

- throughput (RPS): `[TODO]`
- avg latency: `[TODO]`
- p95/p99 latency: `[TODO]`
- CPU usage: `[TODO]`
- memory allocation / GC behavior: `[TODO]`

### Results table (template)

| Metric | Baseline | Batched | Delta |
|---|---:|---:|---:|
| Throughput (RPS) | `[TODO]` | `[TODO]` | `[TODO]` |
| Avg Latency (ms) | `[TODO]` | `[TODO]` | `[TODO]` |
| P95 Latency (ms) | `[TODO]` | `[TODO]` | `[TODO]` |
| P99 Latency (ms) | `[TODO]` | `[TODO]` | `[TODO]` |
| CPU (%) | `[TODO]` | `[TODO]` | `[TODO]` |
| Memory / GC | `[TODO]` | `[TODO]` | `[TODO]` |

### Early observations (template wording)

So far, the biggest visible benefit seems to come from processing the entire batch at once, not from any one line-level trick.

The pattern appears to improve throughput and latency under concurrent load, but I still need to validate this with repeatable runs and multiple batch sizes.

[Add charts and exact commentary after final runs]

---

## What I learned (so far)

1. **Batching changes the economics of work.**  
   Even before deep optimization, doing work once per batch instead of once per request can move the needle.

2. **DOD thinking is useful beyond game dev.**  
   Looking at memory access and data shape helped me reason about backend compute paths more clearly.

3. **Trade-offs are real.**  
   Batching introduces queueing windows, mapping complexity, and operational tuning (batch size, timeout, fairness).

4. **Measurement has to guide confidence.**  
   Without clean benchmarks, it is easy to over-claim.

---

## Conclusion

This was an experiment to learn DOD and request coalescing in a backend API context, and it gave me enough signal to keep going.

I am still learning, and I do **not** want to present this as "the correct way." This is my current attempt based on what I understood and implemented so far.

If you have experience with DOD, batching, or backend performance tuning, I would genuinely love feedback:

- What did I miss?
- What would you measure differently?
- Where could this design break at scale?
- What would you change in the architecture?

I will update this article with final benchmark numbers and deeper analysis once my perf runs are done.

---

## Suggested Medium title options

- `I Tried Request Batching + DOD in a Backend API (Early Results)`
- `Learning DOD in Backend Services: My Request Coalescing Experiment`
- `Can Request Batching Improve API Performance? A Hands-On Attempt`

## Optional subtitle ideas

- `An engineer's learning journey with throughput, latency, and data-oriented thinking`
- `Not a final answer, but a real experiment with real trade-offs`
