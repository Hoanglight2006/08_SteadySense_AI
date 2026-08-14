# P3 Scope And Reuse Plan

## Final Positioning

P3 is a signal-quality-aware fusion layer for reliable edge context recognition.

It is not:

- an on-device SLM action-decision system;
- a multimodal emotion-recognition deployment survey;
- a pure self-supervised HAR encoder paper;
- a Watch-to-phone live-sync paper.

## Relationship To Existing Projects

- Previous HAR/self-supervised work can provide encoders or embeddings.
- DuongExperiment can provide Watch/Phone latency logging style and transport code.
- MultimodalEmotionSmallDevice1 can provide deployment reporting checklist ideas.
- OnHand6 can consume P3 outputs as structured labels with reliability metadata.

## Reuse Targets

Reusable later, when available:

- P1 or HAR embeddings exported as NPZ by split.
- OnHand / OnHand2 / OnHand3 model labels or embeddings.
- DuongExperiment Android/Wear OS timestamp logs.
- Real signal-quality proxies from wearable streams.

## P3 Contribution Boundary

P3 should contribute:

- degradation manifest and generator;
- per-modality quality estimator;
- quality-gated fusion;
- calibrated confidence and abstention;
- robustness, risk-coverage, and edge-cost reporting;
- downstream reliability outputs for OnHand6 or similar reasoning layers.

