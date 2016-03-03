# HandOver
Android version of Apple's HandOff

See Simple demo @
<https://youtu.be/jPpmWsLyzR4>

Maps demo @
<https://youtu.be/3GMcQ2sMLig>

This HandOver repo consists of HandOver Remote Service and Simple Sample.
This apk must be installed on your handset to serve HandOver-maps sample.

Unlike iPhone, Many of android devices yet have BLE advertisement capability thus it does not discover your near by device automatially.
Hard wire BT(BLE) mac addrs in HandOverService.java. Or take BT(classic) paring code from InstantHotSpot.

I still do not have a pair of Android handsets which is capable of emitting BLE advertisement to let client side to discover BLE service.
Thus not supported.
