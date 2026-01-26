import os
import re
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin
from time import sleep 
from typing import Optional

# --- Global Configuration ---
BASE = "https://comicbookplus.com/"

# 1. FIX: Add a User-Agent to mimic a standard web browser.
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
}

# 2. Add a delay to avoid overloading the server.
DELAY_SECONDS = 1.5 
# --- Core Functions ---

def download_page_image(url: str, path: str) -> bool:
    """Helper function to download a single image page."""
    try:
        # Pass the HEADERS dictionary to the download request
        download_response = requests.get(url, stream=True, headers=HEADERS)
        
        # Check the download status before writing (raises HTTPError for 4xx/5xx)
        download_response.raise_for_status() 

        with open(path, "wb") as f:
            for chunk in download_response.iter_content(1024):
                f.write(chunk)
        
        return True
    except requests.exceptions.RequestException as e:
        print(f"  [ERROR] Failed to download {url}: {e}")
        return False
    except Exception as e:
        print(f"  [ERROR] An unexpected error occurred during page download: {e}")
        return False

def download_issue_by_cid(cid, outdir="cid_range_comics"):
    """
    Downloads a single comic issue by its CID using the page-by-page image 
    scraping method derived from the Java script analysis.
    """
    issue_url = f"{BASE}?cid={cid}"
    print(f"\nProcessing CID={cid}: {issue_url}")

    r = requests.get(issue_url, headers=HEADERS) 
    
    if r.status_code != 200:
        print(f"❌ Issue with CID {cid} not found (HTTP Status: {r.status_code}).")
        return

    soup = BeautifulSoup(r.text, "html.parser")

    # 1. Extract issue title for folder/file naming
    title_tag = soup.find("h1")
    issue_title = title_tag.text.strip() if title_tag else f"Comic_{cid}"
    
    # Sanitize the title
    safe_title = re.sub(r"[\\/*?<>|:]", "_", issue_title).strip()
    
    # 2. Extract crucial elements (from Java code insight):
    # Base image URL (from the 'src' attribute of the main comic viewer image)
    main_comic_img = soup.find('img', id='maincomic')
    
    # Total number of pages
    page_span = soup.find('span', itemprop='numberOfPages')
    
    # --- Validation ---
    if not main_comic_img or not main_comic_img.get('src'):
        print("❌ Could not locate the main comic image source (#maincomic).")
        return

    if not page_span or not page_span.text.isdigit():
        print("❌ Could not locate or parse the number of pages.")
        return

    # --- Setup and Extraction ---
    base_image_url = main_comic_img.get('src')
    try:
        total_pages = int(page_span.text)
    except ValueError:
        print("❌ Failed to convert page number text to integer.")
        return

    # 3. Determine download directory and base image URL parts
    download_dir = os.path.join(outdir, f"{safe_title}_{cid}")
    os.makedirs(download_dir, exist_ok=True)
    print(f"✅ Issue title: {issue_title} ({total_pages} pages)")
    print(f"💾 Saving pages to: {download_dir}")
    
    # Logic for URL Reconstruction (as deduced from the Java code):
    # If base_image_url is 'http://.../12345/0.jpg'
    # prefix_path should be 'http://.../12345/'
    # extension should be '.jpg'
    
    prefix_path = base_image_url[:base_image_url.rfind('/') + 1] # Path up to the last slash
    
    # Safely extract the extension (e.g., '.jpg')
    extension_match = re.search(r'(\.[a-zA-Z0-9]+)$', base_image_url)
    extension = extension_match.group(1) if extension_match else ".jpg" # Default to .jpg if complex URL

    downloaded_count = 0
    
    # Pages usually start at 0 or 1. We will assume page 0 is the cover and iterate up to total_pages - 1.
    # The Java script iterated from i=0 up to pagenum-1, so we follow that range.
    for i in range(total_pages): 
        # Reconstruct the page URL: {prefix_path}{page_number}{extension}
        page_url = f"{prefix_path}{i}{extension}"
        
        # Create a safe file name for the page (e.g., ComicTitle_CID_001.jpg)
        page_filename = f"{safe_title}_{cid}_{i:03d}{extension}"
        save_path = os.path.join(download_dir, page_filename)
        
        print(f"  ⬇️ Downloading Page {i+1}/{total_pages} → {page_url}")

        if download_page_image(page_url, save_path):
            downloaded_count += 1
            
        sleep(DELAY_SECONDS / 2) # Shorter sleep for pages within an issue
    
    print(f"✅ Finished CID {cid}. Downloaded {downloaded_count} of {total_pages} pages.")


def download_range(i, j):
    """Downloads all comics with CIDs from i to j (inclusive)."""
    print(f"📚 Downloading comics from CID {i} to {j}")

    for cid in range(i, j + 1):
        try:
            download_issue_by_cid(cid)
        except Exception as e:
            print(f"⚠️ Error downloading CID {cid}: {e}")
            
        # Add mandatory sleep between *issues* to prevent IP banning.
        sleep(DELAY_SECONDS)

    print("\n🎉 Done downloading CID range.")


if __name__ == "__main__":
    # Hardcode here; update whenever you want.
    START_CID = 1210 # Checking a small range around the failed CID
    END_CID = 1220

    download_range(START_CID, END_CID)