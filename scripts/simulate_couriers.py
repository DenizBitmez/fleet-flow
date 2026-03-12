import requests
import random
import time
import uuid

# Configuration
API_URL = "http://localhost:8080/courier/location"
NUM_COURIERS = 100
COURIER_IDS = [str(uuid.uuid4()) for _ in range(NUM_COURIERS)]

# Base coordinates for Istanbul
BASE_LAT = 41.0082
BASE_LON = 28.9784

def simulate_couriers():
    print(f"Starting simulation for {NUM_COURIERS} couriers...")
    while True:
        for courier_id in COURIER_IDS:
            lat = BASE_LAT + random.uniform(-0.05, 0.05)
            lon = BASE_LON + random.uniform(-0.05, 0.05)
            
            payload = {
                "courierId": courier_id,
                "latitude": lat,
                "longitude": lon,
                "timestamp": int(time.time() * 1000)
            }
            
            try:
                # Gateway is at 8080, route is /courier/location
                response = requests.post(API_URL, json=payload)
                if response.status_code != 200:
                    print(f"Error for {courier_id}: {response.status_code}")
            except Exception as e:
                print(f"Request failed: {e}")
        
        print(f"Batch update sent at {time.ctime()}")
        time.sleep(3)

if __name__ == "__main__":
    simulate_couriers()
