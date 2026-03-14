import { useState, useEffect } from 'react'
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet'
import L from 'leaflet'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { Truck, MapPin, Activity, ShieldCheck } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import './App.css'

// Custom Courier Icon
const courierIcon = new L.Icon({
    iconUrl: 'https://cdn-icons-png.flaticon.com/512/2972/2972185.png',
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32]
})

function App() {
  const [couriers, setCouriers] = useState({})
  const [assignments, setAssignments] = useState([])
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    // Fetch initial locations
    fetch('/courier/locations')
      .then(res => res.json())
      .then(data => {
        const initialCouriers = {}
        data.forEach(c => {
          initialCouriers[c.courierId] = c
        })
        setCouriers(initialCouriers)
      })
      .catch(err => console.error('Error fetching initial locations:', err))
  }, [])

  useEffect(() => {
    let stompClient = null;
    let isMounted = true;

    const connectWebSocket = () => {
      const socket = new SockJS('/ws-tracking');
      stompClient = Stomp.over(socket);
      
      stompClient.debug = () => {};

      stompClient.connect({}, 
        (frame) => {
          if (!isMounted) {
            stompClient.disconnect();
            return;
          }
          
          setConnected(true);
          stompClient.subscribe('/topic/locations', (message) => {
            const update = JSON.parse(message.body);
            setCouriers(prev => ({ ...prev, [update.courierId]: update }));
          });

          stompClient.subscribe('/topic/assignments', (message) => {
            const assignment = JSON.parse(message.body);
            setAssignments(prev => {
              const exists = prev.some(a => a.orderId === assignment.orderId);
              if (exists) return prev;
              return [assignment, ...prev].slice(0, 5);
            });
          });
        },
        (error) => {
          console.error('STOMP error:', error);
          if (isMounted) setConnected(false);
        }
      );
    };

    connectWebSocket();

    return () => {
      isMounted = false;
      if (stompClient) {
        try {
          stompClient.disconnect();
        } catch (e) {
          console.error('Disconnect error:', e);
        }
      }
    };
  }, [])



  const courierList = Object.values(couriers)

  return (
    <div className="app-container">
      {/* Map Background */}
      <div className="map-container">
        <MapContainer center={[41.0082, 28.9784]} zoom={13} scrollWheelZoom={true} style={{ height: '100%', width: '100%' }}>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
          />
          {courierList.map((courier) => (
            <Marker 
              key={courier.courierId} 
              position={[courier.latitude, courier.longitude]}
              icon={courierIcon}
            >
              <Popup>
                <div className="popup-content">
                  <strong>Courier:</strong> {courier.courierId.substring(0, 8)}<br/>
                  <strong>Last Update:</strong> {new Date(courier.timestamp).toLocaleTimeString()}
                </div>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>

      {/* UI Overlays */}
      <div className="overlay">
        <motion.div 
          initial={{ x: -100, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          className="glass stats-card"
        >
          <div className="header">
            <h1 className="title">Fleet Flow</h1>
            <div className={`status-dot ${connected ? 'online' : 'offline'}`}></div>
          </div>
          
          <div className="stats-grid">
            <div className="stat-item">
              <Truck size={20} className="icon-p" />
              <div>
                <span className="label">Active Couriers</span>
                <span className="value">{courierList.length}</span>
              </div>
            </div>
            <div className="stat-item">
              <Activity size={20} className="icon-p" />
              <div>
                <span className="label">Live Updates</span>
                <span className="value">Real-time</span>
              </div>
            </div>
          </div>
        </motion.div>

        <motion.div 
          initial={{ y: 100, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          className="glass assignment-panel"
        >
          <h3><ShieldCheck size={18} /> Recent Matching</h3>
          <div className="assignment-list">
            <AnimatePresence>
              {assignments.length === 0 ? (
                <p className="placeholder">Waiting for orders...</p>
              ) : (
                assignments.map((asgn, idx) => (
                  <motion.div 
                    key={asgn.orderId}
                    initial={{ scale: 0.8, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="assignment-item"
                  >

                    <div className="asgn-info">
                      <span className="asgn-id">Order #{asgn.orderId}</span>
                      <span className="asgn-courier">Courier {asgn.courierId.substring(0, 5)}...</span>
                    </div>
                    <div className="asgn-badge">MATCHED</div>
                  </motion.div>
                ))
              )}
            </AnimatePresence>
          </div>
        </motion.div>
      </div>
    </div>
  )
}

export default App
