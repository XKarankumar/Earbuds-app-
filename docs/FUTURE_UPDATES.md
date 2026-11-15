# Future Updates & Modular Expansion Guide

The `:core` module defines extension contracts in `FutureArchitecture.kt` so new premium modules can be added without refactoring existing screens:

1. **Multiple Subwoofers (`MultiSubwooferBus`)**: Implement independent LFE crossover, phase, and millisecond delay alignment per subwoofer node.
2. **Multi-Room Audio (`MultiRoomNodeManager`)**: Add local network zone clock synchronization and latency offsets across room speakers.
3. **AI Acoustic Scene Analysis (`AiMusicAnalysisHook`)**: Plug in an on-device or cloud acoustic analyzer to classify dynamic range and recommend EQ curves.
4. **Additional DJ DSP Effects (`DjEffectPlugin`)**: Register phaser, flanger, bitcrusher, or trance-gate processors on the DJ stereo bus.
5. **Custom RGB Scripts (`CustomRgbAnimationScript`)**: Implement custom procedural LED strip renderers alongside the 108 built-in effects.
6. **Hardware Driver Bridges (`HardwareDriverPlugin`)**: Add AVR IP/Telnet control, Matter/Thread lighting, or licensed OEM ANC/Dolby SDK bridges.
