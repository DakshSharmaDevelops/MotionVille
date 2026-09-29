(() => {
    const previews = document.querySelectorAll("video[data-video-thumbnail]");

    function loadPreview(video) {
        video.addEventListener("loadedmetadata", () => {
            if (Number.isFinite(video.duration) && video.duration > 0) {
                video.currentTime = Math.min(1, video.duration / 2);
            }
        }, { once: true });

        video.addEventListener("seeked", () => {
            video.pause();
            video.classList.add("ready");
        }, { once: true });

        video.addEventListener("error", () => {
            video.remove();
        }, { once: true });

        video.preload = "metadata";
        video.load();
    }

    if ("IntersectionObserver" in window) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    observer.unobserve(entry.target);
                    loadPreview(entry.target);
                }
            });
        }, { rootMargin: "200px" });
        previews.forEach((video) => observer.observe(video));
    } else {
        previews.forEach(loadPreview);
    }
})();
