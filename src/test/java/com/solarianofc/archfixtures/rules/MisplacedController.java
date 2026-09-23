package com.solarianofc.archfixtures.rules;

import org.springframework.web.bind.annotation.RestController;

/** Fixture for the "controllers reside in internal.web" rule (D-116): deliberately outside internal.web. */
@RestController
public class MisplacedController {}
