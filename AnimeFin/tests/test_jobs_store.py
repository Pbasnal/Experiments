from app.storage.jobs import JobsStore, NewJob


def test_jobs_store_create_and_get(tmp_path):
    db_path = tmp_path / "jobs.sqlite3"
    store = JobsStore(db_path)
    ids = store.create_jobs(
        [
            NewJob(
                show_id="s1",
                show_title="Title One",
                episode="1",
                mode="sub",
                quality="best",
                output_path=str(tmp_path / "downloads" / "ep1.mp4"),
            )
        ]
    )
    assert len(ids) == 1

    job = store.get_job(ids[0])
    assert job is not None
    assert job["show_id"] == "s1"
    assert job["status"] == "queued"
    assert any(evt["message"] == "Job queued" for evt in job["events"])
