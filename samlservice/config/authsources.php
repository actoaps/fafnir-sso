<?php

$config = array(

    // Password: secret
    'admin' => array(
        'core:AdminPassword',
    ),

    'example-userpass' => array(
        'exampleauth:UserPass',
        'user1:pass' => array(
            'uid' => array('1'),
            'eduPersonAffiliation' => array('group1'),
            'email' => 'user1@acto.dk',
            'name' => 'User One',
        ),
        'user2:pass' => array(
            'uid' => array('2'),
            'eduPersonAffiliation' => array('group2'),
            'email' => 'user2@acto.dk',
            'name' => 'User Two',
        ),
        'acto:pass' => array(
            'uid' => array('3'),
            'eduPersonAffiliation' => array('group3'),
            'email' => 'acto@acto.dk',
            'name' => 'Acto',
        ),
        'niels:pass' => array(
            'uid' => array('4'),
            'eduPersonAffiliation' => array('group4'),
            'email' => 'niels@acto.dk',
            'name' => 'Niels',
        ),
        'mikkel:pass' => array(
            'uid' => array('5'),
            'eduPersonAffiliation' => array('group5'),
            'email' => 'mikkel@acto.dk',
            'name' => 'Mikkel',
        ),
    ),
);
