#!/usr/bin/env python3
"""Simulate multiple Unity sensors publishing at a fixed Hz over MQTT."""

from __future__ import annotations

import argparse
import json
import sys
import time

try:
    import paho.mqtt.client as mqtt
except ImportError:
    print("Install paho-mqtt: pip install paho-mqtt", file=sys.stderr)
    sys.exit(1)

SENSORS = [
    (
        "Seismic",
        "ST_01",
        {
            "accelerationX_g": 0.018,
            "accelerationY_g": 0.025,
            "accelerationZ_g": 0.011,
        },
    ),
    (
        "Gnss",
        "GNSS_01",
        {
            "displacementX_m": 0.12,
            "displacementY_m": 0.03,
            "displacementZ_m": 0.0,
        },
    ),
    (
        "DartBuoy",
        "DB_01",
        {
            "pressureChange_Pa": 340.5,
            "estimatedSeaSurfaceHeight_m": 0.28,
            "reportingMode": 0,
        },
    ),
]


def main() -> None:
    parser = argparse.ArgumentParser(description="Simulate high-frequency sensor MQTT traffic")
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--port", type=int, default=11883)
    parser.add_argument("--hz", type=float, default=60.0, help="Readings per second per sensor")
    parser.add_argument("--duration", type=float, default=10.0, help="Simulation length in seconds")
    args = parser.parse_args()

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.connect(args.host, args.port)
    client.loop_start()

    period = 1.0 / args.hz
    start = time.perf_counter()
    next_tick = start
    sent = 0

    print(
        f"Simulating {len(SENSORS)} sensors at {args.hz} Hz for {args.duration}s "
        f"({int(args.hz * args.duration * len(SENSORS))} messages expected)..."
    )

    try:
        while True:
            elapsed = time.perf_counter() - start
            if elapsed >= args.duration:
                break

            for reading_type, station_id, fields in SENSORS:
                payload = {
                    "stationId": station_id,
                    "timestamp": round(elapsed, 4),
                    **fields,
                }
                topic = f"sensors/{reading_type}/readings"
                client.publish(topic, json.dumps(payload), qos=0)
                sent += 1

            next_tick += period
            sleep_for = next_tick - time.perf_counter()
            if sleep_for > 0:
                time.sleep(sleep_for)
    finally:
        client.loop_stop()
        client.disconnect()

    print(f"Done. Published {sent} MQTT messages.")


if __name__ == "__main__":
    main()
