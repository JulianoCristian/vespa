// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#include "base.h"

namespace search {

Object::~Object() { }

vespalib::string Object::toString() const
{
    return vespalib::string("");
}

}
