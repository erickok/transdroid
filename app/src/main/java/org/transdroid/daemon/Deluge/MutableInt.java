package org.transdroid.daemon.Deluge;

/**
 * Used to count torrents in labels.
 */
class MutableInt {

  int value = 1;

  MutableInt(int value) {
    this.value = value;
  }

  void increment() {
    ++value;
  }

  int get() {
    return value;
  }
}
