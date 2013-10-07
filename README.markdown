# NOAA ISD Lite

## Introduction

Scala library for downloading historical weather data from NOAA ISD Lite.

## Example

```
import us.tanimoto.NoaaIsdLite

// Madison, WI
val usaf = "726410"
val wban = "14837"
val year = 2012

val noaa = new NoaaIsdLite()

// List weather stations for 2012
val list = noaa.listByYear(year)

// Download weather data
val data = noaa.downloadWeather(usaf, wban, year)

// Close connection
noaa.close()
```

## License

MIT
