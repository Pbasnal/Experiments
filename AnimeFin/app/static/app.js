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

/** AnimePahe base URL UI — runs before search UI so Save still works if other controls are missing. */
async function initAnimepaheSettings() {
  const animepaheBaseEl = byId("animepahe-base-url");
  const animepaheSaveBtn = byId("animepahe-save-base");
  const animepahePathEl = byId("animepahe-config-path");
  const animepaheOutEl = byId("animepahe-settings-output");
  if (!animepaheBaseEl || !animepaheSaveBtn) return;

  async function loadAnimepaheSettings() {
    try {
      const s = await getJson("/api/settings/animepahe");
      animepaheBaseEl.value = s.base_url || "";
      if (animepahePathEl) {
        animepahePathEl.textContent = s.config_path
          ? `Config file: ${s.config_path} (under the hidden .config folder inside your ANIMEPAHE_DL_HOME directory)`
          : "";
      }
      if (animepaheOutEl) animepaheOutEl.textContent = "";
    } catch (error) {
      if (animepaheOutEl) animepaheOutEl.textContent = error.message;
    }
  }

  animepaheSaveBtn.addEventListener("click", async () => {
    if (animepaheOutEl) animepaheOutEl.textContent = "";
    try {
      await getJson("/api/settings/animepahe", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ base_url: animepaheBaseEl.value.trim() }),
      });
      if (animepaheOutEl) animepaheOutEl.textContent = "Saved.";
      await loadAnimepaheSettings();
    } catch (error) {
      if (animepaheOutEl) animepaheOutEl.textContent = error.message;
    }
  });
  await loadAnimepaheSettings();
}

async function initSearchPage() {
  await initAnimepaheSettings();

  const queryEl = byId("query");
  if (!queryEl) return;

  const downloaderEl = byId("downloader");
  const downloaderHintEl = byId("downloader-hint");
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

  function syncDownloaderHint() {
    if (!downloaderEl || !downloaderHintEl) return;
    downloaderHintEl.hidden = downloaderEl.value === "ani_cli";
  }
  downloaderEl.addEventListener("change", syncDownloaderHint);
  syncDownloaderHint();

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
      episodesEl.value = data.episodes.join(",");
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
          downloader: downloaderEl.value,
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
    try {
      const data = await getJson("/api/downloads");
      jobsBody.innerHTML = "";
      (data.jobs || []).forEach((job) => {
      const tr = document.createElement("tr");
      const progress = typeof job.progress_pct === "number" ? `${job.progress_pct.toFixed(1)}%` : "0%";
      const dl = job.downloader === "animepahe_dl" ? "animepahe-dl" : "ani-cli";
      tr.innerHTML = `
        <td>${job.id}</td>
        <td>${job.show_title}</td>
        <td>${job.episode}</td>
        <td>${dl}</td>
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
    } catch (error) {
      jobsBody.innerHTML = "";
      const tr = document.createElement("tr");
      tr.innerHTML = `<td colspan="7">${error.message}</td>`;
      jobsBody.appendChild(tr);
    }
  }

  async function refreshMedia() {
    try {
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
    } catch (error) {
      mediaList.innerHTML = "";
      const li = document.createElement("li");
      li.textContent = `Could not load media list: ${error.message}`;
      mediaList.appendChild(li);
    }
  }

  refreshJobsBtn.addEventListener("click", refreshJobs);
  refreshMediaBtn.addEventListener("click", refreshMedia);
  await refreshMedia();
  await refreshJobs();
  setInterval(refreshJobs, 2000);
  setInterval(refreshMedia, 5000);
}

initSearchPage();
initDownloadsPage();
