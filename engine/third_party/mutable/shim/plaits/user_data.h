// Pocket Studio shim: Plaits stores user wavetables / DX7 banks in the module's flash.
// On the phone there is none: every engine uses its built-in data.
#ifndef PLAITS_USER_DATA_H_
#define PLAITS_USER_DATA_H_

#include <cstddef>
#include <cstdint>

namespace plaits {

class UserData {
 public:
  inline const uint8_t* ptr(int) const { return NULL; }
};

}  // namespace plaits

#endif  // PLAITS_USER_DATA_H_
