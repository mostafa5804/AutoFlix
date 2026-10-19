(() => {
  if (window.__autoFlixPlayer) return window.__autoFlixPlayer.promote();

  const playerSelector = '.html5-video-player,.video-js,.jwplayer,.plyr,.mejs__container,' +
    '.mejs-container,.flowplayer,[data-player],[data-player-container]';
  const controlsSelector = '.ytp-chrome-bottom,.vjs-control-bar,.jw-controlbar,.plyr__controls,' +
    '.mejs__controls,.mejs-controls,[data-player-controls],.player-controls,.video-controls';
  const skipSelector = '.ytp-ad-skip-button,.ytp-ad-skip-button-modern,' +
    '.ytp-skip-ad-button,.videoAdUiSkipButton,.jw-skip,.vjs-skip-ad,' +
    '[data-skip-ad],[data-testid="skip-ad"]';
  const candidates = [];
  const visible = node => {
    const rect = node.getBoundingClientRect();
    const style = node.ownerDocument.defaultView.getComputedStyle(node);
    return node.isConnected && rect.width > 0 && rect.height > 0 &&
      style.display !== 'none' && style.visibility !== 'hidden' && Number(style.opacity) > 0;
  };
  const collect = (doc, frames) => {
    doc.querySelectorAll('video').forEach(video => {
      if (!visible(video)) return;
      const rect = video.getBoundingClientRect();
      candidates.push({ video, frames, area: rect.width * rect.height });
    });
    doc.querySelectorAll('iframe').forEach(frame => {
      try {
        if (visible(frame) && frame.contentDocument) collect(frame.contentDocument, frames.concat(frame));
      } catch (_) { /* Cross-origin frames are inaccessible. */ }
    });
  };
  collect(document, []);
  const target = candidates.find(item => !item.video.paused && !item.video.ended) ||
    candidates.sort((left, right) => right.area - left.area)[0];
  if (!target) return 'no-video';

  const originalVideo = target.video;
  const doc = originalVideo.ownerDocument;
  let root = originalVideo.closest(playerSelector);
  if (!root) {
    let ancestor = originalVideo.parentElement;
    for (let depth = 0; ancestor && ancestor !== doc.body && depth < 5; depth++) {
      if (ancestor.querySelector(controlsSelector) ||
          ancestor.querySelector('button,[role="button"],input[type="range"]')) {
        root = ancestor;
        break;
      }
      ancestor = ancestor.parentElement;
    }
  }
  root = root || (originalVideo.parentElement !== doc.body ? originalVideo.parentElement : originalVideo);
  const savedStyles = new Map();
  const normalizedStyles = doc.createElement('div').style;
  const savedControls = new Map();
  const clickedButtons = new Set();
  let active = true;
  let fullscreen = false;
  let resumeAfterSkipUntil = 0;
  let video = originalVideo;

  const setStyle = (node, property, value) => {
    if (!savedStyles.has(node)) savedStyles.set(node, node.getAttribute('style'));
    // Compare serialized CSS values so normalization cannot retrigger the observer.
    normalizedStyles.setProperty(property, value);
    if (node.style.getPropertyValue(property) !== normalizedStyles.getPropertyValue(property) ||
        node.style.getPropertyPriority(property) !== 'important') {
      node.style.setProperty(property, value, 'important');
    }
  };
  const play = current => {
    try {
      const request = current.play();
      if (request && request.catch) request.catch(() => {});
    } catch (_) {}
  };
  const currentVideo = () => {
    const videos = root === originalVideo ? [originalVideo] : Array.from(root.querySelectorAll('video'));
    return videos.find(item => visible(item) && !item.paused && !item.ended) ||
      (video.isConnected && visible(video) ? video : videos.find(visible));
  };
  const expandSurface = surface => {
    setStyle(surface, 'position', 'fixed');
    setStyle(surface, 'inset', '0px');
    setStyle(surface, 'width', '100vw');
    setStyle(surface, 'height', '100vh');
    setStyle(surface, 'max-width', 'none');
    setStyle(surface, 'max-height', 'none');
    setStyle(surface, 'margin', '0');
    setStyle(surface, 'padding', '0');
    setStyle(surface, 'border', '0');
    setStyle(surface, 'background', '#000');
    setStyle(surface, 'z-index', '2147483647');
    // Transformed ancestors otherwise constrain a fixed-position player.
    for (let ancestor = surface.parentElement; ancestor; ancestor = ancestor.parentElement) {
      setStyle(ancestor, 'transform', 'none');
      setStyle(ancestor, 'filter', 'none');
      setStyle(ancestor, 'perspective', 'none');
      setStyle(ancestor, 'contain', 'none');
      setStyle(ancestor, 'overflow', 'visible');
      setStyle(ancestor, 'z-index', '2147483647');
    }
  };
  const preserveControls = current => {
    const customControls = root === current ? [] : Array.from(root.querySelectorAll(controlsSelector));
    customControls.forEach(controls => {
      if (controls.ownerDocument.defaultView.getComputedStyle(controls).display === 'none') {
        setStyle(controls, 'display', 'flex');
      }
      setStyle(controls, 'opacity', '1');
      setStyle(controls, 'visibility', 'visible');
      setStyle(controls, 'pointer-events', 'auto');
      setStyle(controls, 'transform', 'none');
      setStyle(controls, 'z-index', '2');
    });
    // Leave a site's original controller in charge. Bare videos get native controls.
    const hasSiteControls = customControls.length > 0 ||
      (root !== current && Array.from(root.querySelectorAll('button,[role="button"],input[type="range"]'))
        .some(control => !isSkipButton(control)));
    if (!savedControls.has(current)) savedControls.set(current, current.controls);
    if (!hasSiteControls && !current.controls) current.controls = true;
  };
  const layoutVideo = current => {
    if (root !== current) {
      for (let wrapper = current.parentElement; wrapper && wrapper !== root; wrapper = wrapper.parentElement) {
        setStyle(wrapper, 'width', '100%');
        setStyle(wrapper, 'height', '100%');
        setStyle(wrapper, 'max-width', 'none');
        setStyle(wrapper, 'max-height', 'none');
      }
      setStyle(current, 'position', 'absolute');
      setStyle(current, 'inset', '0px');
      setStyle(current, 'width', '100%');
      setStyle(current, 'height', '100%');
      // Keep the existing controller above the video, rather than behind it.
      setStyle(current, 'z-index', '0');
    }
    setStyle(current, 'max-width', 'none');
    setStyle(current, 'max-height', 'none');
    setStyle(current, 'object-fit', 'contain');
    preserveControls(current);
    if (current.dataset.autoflixFullscreen !== 'true') current.dataset.autoflixFullscreen = 'true';
  };
  const isSkipButton = button => {
    if (button.matches(skipSelector)) return true;
    const label = (button.getAttribute('aria-label') || button.textContent || '')
      .trim().replace(/\s+/g, ' ').replace(/\u200c/g, ' ');
    if (/^(skip ad(?:s|vertisement)?|رد (?:کردن )?(?:آگهی|تبلیغ(?:ات)?)|عبور از (?:آگهی|تبلیغ(?:ات)?)|پرش از (?:آگهی|تبلیغ(?:ات)?))$/i.test(label)) return true;
    const adPlaying = root.matches('.ad-showing,.vjs-ad-playing,.jw-flag-ads,[data-ad-playing="true"]') ||
      video.getAttribute('data-ad-playing') === 'true';
    return adPlaying && /^(skip|رد کردن)$/i.test(label);
  };
  const skipAds = () => {
    clickedButtons.forEach(button => {
      if (!visible(button) || button.disabled || button.getAttribute('aria-disabled') === 'true') {
        clickedButtons.delete(button);
      }
    });
    root.querySelectorAll(skipSelector + ',button,[role="button"]').forEach(button => {
      if (!isSkipButton(button) || !visible(button) || button.disabled ||
          button.getAttribute('aria-disabled') === 'true' || clickedButtons.has(button)) return;
      clickedButtons.add(button);
      resumeAfterSkipUntil = Date.now() + 3000;
      button.click();
      const current = currentVideo();
      if (current) play(current);
    });
  };
  const refresh = () => {
    if (!active) return;
    const current = currentVideo();
    if (current) {
      video = current;
      if (fullscreen) layoutVideo(current);
      if (Date.now() < resumeAfterSkipUntil && current.paused) play(current);
    }
    skipAds();
  };
  const observer = new doc.defaultView.MutationObserver(refresh);
  observer.observe(root, { childList: true, subtree: true, attributes: true, characterData: true });
  const interval = setInterval(refresh, 250);
  const onLoadedData = () => {
    if (active && Date.now() < resumeAfterSkipUntil) refresh();
  };
  const onPlaying = () => { resumeAfterSkipUntil = 0; };
  const onControlInteraction = event => {
    const button = event.target.closest?.(skipSelector + ',button,[role="button"]');
    if (!button || !isSkipButton(button)) resumeAfterSkipUntil = 0;
  };
  root.addEventListener('loadeddata', onLoadedData, true);
  root.addEventListener('playing', onPlaying, true);
  root.addEventListener('pointerdown', onControlInteraction, true);
  root.addEventListener('click', onControlInteraction, true);

  const state = {
    promote() {
      if (!active) return 'no-video';
      refresh();
      if (video.paused || video.ended || video.readyState < 2) {
        play(video);
        return 'play-requested';
      }
      if (!fullscreen) {
        fullscreen = true;
        target.frames.forEach(expandSurface);
        expandSurface(root);
        setStyle(doc.documentElement, 'overflow', 'hidden');
        setStyle(doc.body, 'overflow', 'hidden');
      }
      layoutVideo(video);
      return 'playing';
    },
    stop() {
      active = false;
      observer.disconnect();
      clearInterval(interval);
      root.removeEventListener('loadeddata', onLoadedData, true);
      root.removeEventListener('playing', onPlaying, true);
      root.removeEventListener('pointerdown', onControlInteraction, true);
      root.removeEventListener('click', onControlInteraction, true);
      savedStyles.forEach((style, node) => {
        if (style === null) node.removeAttribute('style');
        else node.setAttribute('style', style);
      });
      savedControls.forEach((controls, current) => {
        current.controls = controls;
        delete current.dataset.autoflixFullscreen;
      });
      delete window.__autoFlixPlayer;
    }
  };
  window.__autoFlixPlayer = state;
  return state.promote();
})()
