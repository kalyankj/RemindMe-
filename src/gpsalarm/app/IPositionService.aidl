package gpsalarm.app;

import android.location.Location;

interface IPositionService {
	Location getLastLocation();
}