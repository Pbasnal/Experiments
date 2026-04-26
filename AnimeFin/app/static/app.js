function byId(id) {
  return document.getElementById(id);
}

async function getJson(url, options = {}) {
  const response = await fetch(url, options);
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || "Request failed");
  }
  return data;
}

async function initSearchPage() {
  const queryEl = byId("query");
  if (!queryEl) return;

  const modeEl = byId("mode");
  const searchBtn = byId("search-btn");
  const resultsEl = byId("search-results");
  const showIdEl = byId("show-id");
  const showTitleEl = byId("show-title");
  const loadEpisodesBtn = byId("load-episodes-btn");
  const episodesEl = byId("episodes");
  const qualityEl = byId("quality");
  const enqueueBtn = byId("enqueue-btn");
  const outputEl = byId("action-output");

  searchBtn.addEventListener("click", async () => {
    const query = queryEl.value.trim();
    if (!query) return;
    outputEl.textContent = "";
    resultsEl.innerHTML = "";

    try {
      const data = await getJson(`/api/search?q=${encodeURIComponent(query)}&mode=${encodeURIComponent(modeEl.value)}`);
      if (!data.results.length) {
        resultsEl.innerHTML = "<li>No shows found.</li>";
        return;
      }
      data.results.forEach((show) => {
        const li = document.createElement("li");
        const btn = document.createElement("button");
        btn.textContent = "Select";
        btn.addEventListener("click", () => {
          showIdEl.value = show.id;
          showTitleEl.value = show.title;
        });
        li.innerHTML = `<strong>${show.title}</strong> (${show.episode_count} episodes) `;
        li.appendChild(btn);
        resultsEl.appendChild(li);
      });
    } catch (error) {
      outputEl.textContent = error.message;
    }
  });

  loadEpisodesBtn.addEventListener("click", async () => {
    const showId = showIdEl.value.trim();
    if (!showId) return;
    try {
      const data = await getJson(
        `/api/shows/${encodeURIComponent(showId)}/episodes?mode=${encodeURIComponent(modeEl.value)}`
      );
      episodesEl.value = data.episodes.slice(0, 5).join(",");
      outputEl.textContent = `Loaded ${data.episodes.length} episodes`;
    } catch (error) {
      outputEl.textContent = error.message;
    }
  });

  enqueueBtn.addEventListener("click", async () => {
    const showId = showIdEl.value.trim();
    const showTitle = showTitleEl.value.trim();
    const episodes = episodesEl.value
      .split(",")
      .map((e) => e.trim())
      .filter(Boolean);
    if (!showId || !showTitle || !episodes.length) {
      outputEl.textContent = "Select a show and at least one episode.";
      return;
    }
    try {
      const data = await getJson("/api/downloads", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          show_id: showId,
          show_title: showTitle,
          episodes,
          mode: modeEl.value,
          quality: qualityEl.value.trim() || "best",
        }),
      });
      outputEl.textContent = `Queued jobs:\n${data.job_ids.join("\n")}`;
    } catch (error) {
      outputEl.textContent = error.message;
    }
  });
}

async function initDownloadsPage() {
  const jobsBody = byId("jobs-body");
  if (!jobsBody) return;

  const refreshJobsBtn = byId("refresh-jobs-btn");
  const refreshMediaBtn = byId("refresh-media-btn");
  const mediaList = byId("media-list");
  const jobEvents = byId("job-events");

  async function refreshJobs() {
    const data = await getJson("/api/downloads");
    jobsBody.innerHTML = "";
    data.jobs.forEach((job) => {
      const tr = document.createElement("tr");
      const progress = typeof job.progress_pct === "number" ? `${job.progress_pct.toFixed(1)}%` : "0%";
      tr.innerHTML = `
        <td>${job.id}</td>
        <td>${job.show_title}</td>
        <td>${job.episode}</td>
        <td>${job.status}</td>
        <td>${progress}</td>
        <td></td>
      `;
      const actionCell = tr.lastElementChild;

      const detailsBtn = document.createElement("button");
      detailsBtn.textContent = "View";
      detailsBtn.addEventListener("click", async () => {
        const detail = await getJson(`/api/downloads/${encodeURIComponent(job.id)}`);
        jobEvents.textContent = (detail.events || [])
          .map((event) => `[${event.timestamp}] ${event.level}: ${event.message}`)
          .join("\n");
      });
      actionCell.appendChild(detailsBtn);

      if (job.status === "queued" || job.status === "running") {
        const cancelBtn = document.createElement("button");
        cancelBtn.textContent = "Cancel";
        cancelBtn.addEventListener("click", async () => {
          await getJson(`/api/downloads/${encodeURIComponent(job.id)}`, { method: "DELETE" });
          await refreshJobs();
        });
        actionCell.appendChild(cancelBtn);
      }

      jobsBody.appendChild(tr);
    });
  }

  async function refreshMedia() {
    const data = await getJson("/api/media");
    mediaList.innerHTML = "";
    if (!data.items.length) {
      mediaList.innerHTML = "<li>No downloaded files yet.</li>";
      return;
    }
    data.items.forEach((item) => {
      const li = document.createElement("li");
      const button = document.createElement("button");
      button.textContent = "Delete";
      button.addEventListener("click", async () => {
        if (!window.confirm(`Delete ${item.media_id}?`)) return;
        await getJson(`/api/media/${encodeURIComponent(item.media_id)}`, { method: "DELETE" });
        await refreshMedia();
      });
      li.innerHTML = `<code>${item.media_id}</code> `;
      li.appendChild(button);
      mediaList.appendChild(li);
    });
  }

  refreshJobsBtn.addEventListener("click", refreshJobs);
  refreshMediaBtn.addEventListener("click", refreshMedia);
  await refreshJobs();
  await refreshMedia();
  setInterval(refreshJobs, 2000);
}

initSearchPage();
initDownloadsPage();
