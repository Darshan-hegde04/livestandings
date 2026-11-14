import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import './App.css';

// Avoid StrictMode double-mount — it opens/closes EventSource twice and breaks SSE.
createRoot(document.getElementById('root')).render(<App />);
