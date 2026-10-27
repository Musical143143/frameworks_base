# PenguinOS QS panel styles (raw, unmerged)

These 17 SystemUI patches are taken from Project-PenguinOS/frameworks_base, applied in order with `git am`.
They do NOT apply cleanly to Lunaris (lunaris-16.2 / backup-before-lunaris): the first patch alone conflicts in 14 files in the volume dialog.
Merge them manually, build, and test before use. Nothing in this directory is compiled.

Apply: git am -3 patches/penguin-qs-styles/*.patch
