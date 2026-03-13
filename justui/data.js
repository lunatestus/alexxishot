/**
 * Mock data representing a server file system.
 */
const mockFileSystem = {
  "/": [
    { type: "folder", name: "Movies", path: "/Movies" },
    { type: "folder", name: "TV Shows", path: "/TV Shows" },
    { type: "folder", name: "Music", path: "/Music" },
    { type: "file", name: "Welcome Video.mp4", path: "/Welcome Video.mp4", thumb: "https://placehold.co/600x400/121212/007bff?text=Movie+1" },
    { type: "file", name: "ReadMe.txt", path: "/ReadMe.txt", thumb: "https://placehold.co/600x400/121212/007bff?text=Doc" }
  ],
  "/Movies": [
    { type: "file", name: "The Matrix.mp4", path: "/Movies/The Matrix.mp4", thumb: "https://placehold.co/600x400/121212/007bff?text=The+Matrix" },
    { type: "file", name: "Inception.mp4", path: "/Movies/Inception.mp4", thumb: "https://placehold.co/600x400/121212/007bff?text=Inception" },
    { type: "file", name: "Interstellar.mp4", path: "/Movies/Interstellar.mp4", thumb: "https://placehold.co/600x400/121212/007bff?text=Interstellar" }
  ],
  "/TV Shows": [
    { type: "folder", name: "Breaking Bad", path: "/TV Shows/Breaking Bad" },
    { type: "folder", name: "The Wire", path: "/TV Shows/The Wire" }
  ],
  "/TV Shows/Breaking Bad": [
    { type: "file", name: "S01E01.mp4", path: "/TV Shows/Breaking Bad/S01E01.mp4", thumb: "https://placehold.co/600x400/121212/007bff?text=BB+S1E1" },
    { type: "file", name: "S01E02.mp4", path: "/TV Shows/Breaking Bad/S01E02.mp4", thumb: "https://placehold.co/600x400/121212/007bff?text=BB+S1E2" }
  ],
  "/TV Shows/The Wire": [
    { type: "file", name: "S01E01.mp4", path: "/TV Shows/The Wire/S01E01.mp4", thumb: "https://placehold.co/600x400/121212/007bff?text=Wire+S1E1" }
  ],
  "/Music": [
    { type: "file", name: "Song 1.mp3", path: "/Music/Song 1.mp3", thumb: "https://placehold.co/600x400/121212/007bff?text=Music" }
  ]
};

/**
 * Simulate fetching folder contents from a server.
 * @param {string} path 
 * @returns {Promise<Array>}
 */
async function fetchFolder(path) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockFileSystem[path] || []);
    }, 200); // Simulate network latency
  });
}
