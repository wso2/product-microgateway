package custom

default allow = false

allow = true {
    input.headers["custom-foo"] == "custom-bar"
}
