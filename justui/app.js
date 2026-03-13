// Constants
const MIN_GRID_COLS = 1;

// State
let state = {
    currentPath: "/",
    items: [],
    focusArea: "grid", // "sidebar" or "grid" or "header" or "player"
    focusedIndex: 0,
    history: [],
    viewMode: "list", // "grid" or "list"
    playerFocusIndex: 1,
    progress: 22
};

// DOM Elements
const sidebar = document.getElementById('sidebar');
const contentGrid = document.getElementById('content-grid');
const mainContent = document.getElementById('main-content');
const currentPathEl = document.getElementById('current-path');
const viewToggleBtn = document.getElementById('view-toggle');
const playerOverlay = document.getElementById('player-overlay');
const playerTitle = document.getElementById('player-title-text');

/**
 * Initialize the app
 */
async function init() {
    await loadPath("/");
    updateFocus();
    updateViewMode();
    
    // Listen for keyboard events
    window.addEventListener('keydown', handleKeyDown);
    viewToggleBtn.addEventListener('click', toggleViewMode);
    document.querySelectorAll('.player-focusable').forEach((el, index) => {
        el.addEventListener('click', () => {
            state.playerFocusIndex = index;
            const action = el.getAttribute('data-action');
            if (action === 'toggle') {
                togglePlayState();
            } else if (action === 'settings') {
                console.log('Open settings');
            }
            updateFocus();
        });
    });
}

/**
 * Load folder contents and render
 */
async function loadPath(path) {
    state.currentPath = path;
    currentPathEl.textContent = path === "/" ? "" : `/ ${path.replace(/^\//, "")}`;
    
    state.items = await fetchFolder(path);
    renderGrid();
    
    state.focusedIndex = 0;
    state.focusArea = "grid";
    updateFocus();
}

/**
 * Render the grid of files and folders
 */
function renderGrid() {
    contentGrid.innerHTML = '';
    
    state.items.forEach((item, index) => {
        const card = document.createElement('div');
        card.className = 'card';
        card.setAttribute('data-index', index);
        
        card.innerHTML = `
            <div class="card-img-placeholder"></div>
            <div class="card-info">
                <div class="card-title">${item.name}</div>
                <div class="card-type">${item.type.toUpperCase()}</div>
            </div>
        `;
        
        card.onclick = () => handleSelect(index);
        contentGrid.appendChild(card);
    });
}

/**
 * Toggle between grid and list view
 */
function toggleViewMode() {
    state.viewMode = state.viewMode === "grid" ? "list" : "grid";
    updateViewMode();
}

function updateViewMode() {
    const isList = state.viewMode === "list";
    document.body.classList.toggle('list-view', isList);
    contentGrid.classList.toggle('list-view', isList);
    viewToggleBtn.setAttribute('aria-label', isList ? 'Switch to grid view' : 'Switch to list view');
    viewToggleBtn.setAttribute('title', isList ? 'Switch to grid view' : 'Switch to list view');
}

/**
 * Handle selection (Enter key or click)
 */
async function handleSelect(index) {
    const item = state.items[index];
    if (item.type === 'folder') {
        state.history.push(state.currentPath);
        await loadPath(item.path);
    } else {
        playFile(item);
    }
}

/**
 * Play a file
 */
function playFile(item) {
    playerTitle.textContent = item.name;
    state.focusArea = "player";
    state.playerFocusIndex = 1;
    playerOverlay.classList.remove('hidden');
    playerOverlay.setAttribute('aria-hidden', 'false');
    updateFocus();
    updateProgressUI();
    console.log(`Playing: ${item.name}`);
}

function closePlayer() {
    playerOverlay.classList.add('hidden');
    playerOverlay.setAttribute('aria-hidden', 'true');
    state.focusArea = "grid";
    updateFocus();
}

function togglePlayState() {
    playerOverlay.classList.toggle('playing');
}

/**
 * Update focus visuals
 */
function updateFocus() {
    // Clear all focus
    document.querySelectorAll('.focused').forEach(el => el.classList.remove('focused'));
    
    if (state.focusArea === "grid") {
        sidebar.classList.remove('active');
        mainContent.classList.remove('sidebar-open');
        
        const cards = document.querySelectorAll('.card');
        if (cards[state.focusedIndex]) {
            cards[state.focusedIndex].classList.add('focused');
            cards[state.focusedIndex].scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    } else if (state.focusArea === "sidebar") {
        sidebar.classList.add('active');
        mainContent.classList.add('sidebar-open');
        
        const navItems = document.querySelectorAll('.nav-item');
        if (navItems[state.focusedIndex]) {
            navItems[state.focusedIndex].classList.add('focused');
        }
    } else if (state.focusArea === "header") {
        viewToggleBtn.classList.add('focused');
    } else if (state.focusArea === "player") {
        const controls = document.querySelectorAll('.player-focusable');
        controls.forEach(el => el.classList.remove('focused'));
        if (controls[state.playerFocusIndex]) {
            controls[state.playerFocusIndex].classList.add('focused');
        }
    }
}

function getGridCols() {
    if (state.viewMode === "list") {
        return 1;
    }
    const style = getComputedStyle(contentGrid);
    const cols = style.gridTemplateColumns.split(' ').length;
    return Math.max(cols || 0, MIN_GRID_COLS);
}

function updateProgressUI() {
    const controls = document.querySelectorAll('.player-focusable');
    if (controls[0]) {
        controls[0].style.setProperty('--progress', `${state.progress}%`);
        const timeDisplay = controls[0].querySelector('.progress-time span:first-child');
        if (timeDisplay) {
            const totalSeconds = 276; // 4:36
            const currentSeconds = Math.round((state.progress / 100) * totalSeconds);
            const mins = Math.floor(currentSeconds / 60);
            const secs = currentSeconds % 60;
            timeDisplay.textContent = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
        }
    }
}

/**
 * Handle D-Pad / Keyboard Navigation
 */
function handleKeyDown(e) {
    const key = e.key;

    if (state.focusArea === "grid") {
        const gridCols = getGridCols();
        const row = Math.floor(state.focusedIndex / gridCols);
        const col = state.focusedIndex % gridCols;

        switch (key) {
            case "ArrowRight":
                if (col < gridCols - 1 && state.focusedIndex < state.items.length - 1) {
                    state.focusedIndex++;
                }
                break;
            case "ArrowLeft":
                if (col > 0) {
                    state.focusedIndex--;
                } else {
                    // Open sidebar
                    state.focusArea = "sidebar";
                    state.focusedIndex = 0; // Default to first sidebar item
                }
                break;
            case "ArrowDown":
                if (state.focusedIndex + gridCols < state.items.length) {
                    state.focusedIndex += gridCols;
                }
                break;
            case "ArrowUp":
                if (row === 0) {
                    state.focusArea = "header";
                } else if (state.focusedIndex - gridCols >= 0) {
                    state.focusedIndex -= gridCols;
                }
                break;
            case "Enter":
                handleSelect(state.focusedIndex);
                break;
            case "Backspace":
                goBack();
                break;
        }
    } else if (state.focusArea === "sidebar") {
        const navItems = document.querySelectorAll('.nav-item');
        switch (key) {
            case "ArrowDown":
                if (state.focusedIndex < navItems.length - 1) {
                    state.focusedIndex++;
                }
                break;
            case "ArrowUp":
                if (state.focusedIndex > 0) {
                    state.focusedIndex--;
                }
                break;
            case "ArrowRight":
            case "Enter":
                // Exit sidebar to grid
                state.focusArea = "grid";
                state.focusedIndex = 0;
                break;
            case "Backspace":
                state.focusArea = "grid";
                break;
        }
    } else if (state.focusArea === "header") {
        switch (key) {
            case "ArrowDown":
            case "ArrowRight":
            case "Enter":
                if (key === "Enter") {
                    toggleViewMode();
                } else {
                    state.focusArea = "grid";
                }
                break;
            case "ArrowLeft":
                state.focusArea = "grid";
                break;
        }
    } else if (state.focusArea === "player") {
        const controls = document.querySelectorAll('.player-focusable');
        switch (key) {
            case "ArrowRight":
                if (state.playerFocusIndex === 0) {
                    state.progress = Math.min(100, state.progress + 2);
                    updateProgressUI();
                } else if (state.playerFocusIndex < controls.length - 1) {
                    state.playerFocusIndex++;
                }
                break;
            case "ArrowLeft":
                if (state.playerFocusIndex === 0) {
                    state.progress = Math.max(0, state.progress - 2);
                    updateProgressUI();
                } else if (state.playerFocusIndex > 1) {
                    state.playerFocusIndex--;
                }
                break;
            case "ArrowUp":
                // jump to progress bar
                state.playerFocusIndex = 0;
                break;
            case "ArrowDown":
                // jump to first control (play/pause)
                if (state.playerFocusIndex === 0 && controls.length > 1) {
                    state.playerFocusIndex = 1;
                }
                break;
            case "Enter": {
                const action = controls[state.playerFocusIndex]?.getAttribute('data-action');
                if (action === 'toggle') {
                    togglePlayState();
                } else if (action === 'settings') {
                    console.log('Open settings');
                }
                break;
            }
            case "Escape":
            case "Backspace":
                closePlayer();
                break;
        }
    }
    
    updateFocus();
}

/**
 * Handle going back to parent folder
 */
async function goBack() {
    if (state.history.length > 0) {
        const prevPath = state.history.pop();
        await loadPath(prevPath);
    }
}

// Start the app
init();
