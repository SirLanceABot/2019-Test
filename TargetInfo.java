import com.google.gson.Gson;

public class TargetInfo {
	private double COGX;
	private double COGY;
	private double Width;
	private double Area;
	private boolean Fresh;

	public TargetInfo() {
		COGX = -1.;
		COGY = -1.;
		Width = 0;
		Area = 0.;
		Fresh = true;
	}

	public synchronized void set(double aCOGX, double aCOGY, double aWidth, double aArea) {
		COGX = aCOGX;
		COGY = aCOGY;
		Width = aWidth;
		Area = aArea;
		Fresh = true;
	}

	public synchronized void get(TargetInfo aTargetInfo) {
		aTargetInfo.COGX = COGX;
		aTargetInfo.COGY = COGY;
		aTargetInfo.Width = Width;
		aTargetInfo.Area = Area;
		aTargetInfo.Fresh = Fresh;
		Fresh = false;
	}

	public synchronized boolean isFresh() {
		return Fresh;
	}

	public synchronized String toString() {
		return String.format("[TargetInfo] COG x = %f, COG y = %f, Width = %f, Area = %f  %s]", COGX, COGY, Width, Area,
				Fresh ? "FRESH" : "stale");
	}

	public synchronized String toJson() {
		Gson gson = new Gson(); // Or use new GsonBuilder().create();
		String json = gson.toJson(this); // serializes target to Json
		return json;
	}
}
