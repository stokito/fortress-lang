/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import junit.framework.TestCase;

import java.util.Random;

public class BASetDetailedTest extends TestCase {
    //com.sun.fortress.useful.TestCaseWrapper  {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(BATJUTest.class);
    }

    BASet<String> t = new BASet<String>(String.CASE_INSENSITIVE_ORDER);

    /*
    String[] adds = {
            "auk25", "eel83", "gar37", "vole91", "bass28", "giraffe37", "eland64", "octopus34", "buffalo3", "bison28",
            "octopus28", "dog0", "cow61", "elephant20", "armadillo1", "caiman65", "aardwolf37", "deer23", "cow41",
            "dingo79", "nutria18", "terrapin92", "aardvark67", "camel80", "cat13", "anole29", "crocodile63",
            "crocodile86", "dog94", "cavy47", "turtle49", "rat19", "bison28", "auk42", "cow28", "elephant35", "cow59",
            "buffalo1", "crab42", "guanaco21", "camel95", "shark13", "elephant41", "camel41", "cat31", "squid75",
            "piranha36", "beetle96", "bison59", "blackbird17", "elk87", "lamprey20", "rat95", "newt16", "shark29",
            "aardvark60", "bear61", "camel11", "hyena29", "ibis59", "mule51", "mosquito88", "armadillo21",
            "blackbird61", "bluebird46", "dove1", "manatee15", "hedgehog17", "fly63", "aardvark43", "blackbird53",
            "rat86", "dingo56", "armadillo82", "cow36", "kiwi75", "cat14", "camel0", "nutria72", "terrapin6", "kiwi90",
            "gnu52", "gharial52", "camel82", "osprey18", "gnat56", "aardwolf46", "quail59", "cassowary44", "squid60",
            "elephant98", "skink15", "bat82", "crab30", "eland84", "caiman87", "spider32", "deer30", "flea99",
            "bison68", "auk58", "finch74", "narwhal29", "lamprey21", "gharial49", "bison7", "buzzard77", "ant1",
            "camel3", "emu16", "gull24", "dodo51", "dodo68", "dingo12", "eel52", "mosquito53", "cow28", "eland28",
            "gull54", "cow43", "hawk81", "camel7", "narwhal59", "xiphias72", "salamander28", "quagga20", "kangaroo92",
            "sidewinder39", "armadillo82", "gnat62", "cassowary80", "eland69", "iguana38", "gnu63", "buffalo34",
            "vicuna26", "toad31", "kangaroo36", "crab27", "cow99", "bison16", "blackbird6", "cassowary17", "aardvark98",
            "bluebird59", "hedgehog90", "flea63", "sheep6", "quail11", "manatee59", "pigeon86", "gnu26", "giraffe89",
            "tern39", "blackbird1", "kiwi56", "lemur26", "dove7", "pelican96", "anole95", "jaguar91", "dingo1",
            "buffalo90", "deer74", "deer90", "caiman9", "armadillo29", "lamprey57", "koala82", "beetle38", "bat20",
            "sidewinder94", "flea87", "anole55", "cassowary21", "bat8", "aardvark3", "camel24", "vulture33", "iguana44",
            "aardwolf25", "bluebird65", "bluebird9", "cavy29", "quokka40", "armadillo30", "seal66", "finch79",
            "beetle27", "bluebird79", "aardvark85", "crocodile4", "buzzard88", "crab96", "cow56", "dog90", "quagga98",
            "giraffe97", "quokka34", "cow21", "gnat30", "buzzard95", "armadillo43", "auk77", "elk35", "xiphias34",
            "cassowary71", "emu1", "tiger49", "giraffe63", "camel93", "turtle31", "cat28", "guanaco55", "seal64",
            "armadillo8", "gnat1", "manatee35", "rat83", "newt99", "crocodile27", "buzzard23", "unicorn14", "narwhal18",
            "crocodile68", "cow89", "buffalo48", "vicuna17", "dog11", "buzzard42", "caiman12", "quokka23", "rat86",
            "bison48", "elephant95", "cat56", "anole48", "iguana24", "caiman28", "hawk45", "worm85", "pelican11",
            "dog69", "caiman31", "dingo30", "iguana49", "quail6", "blackbird42", "bear65", "cassowary41", "crocodile88",
            "gnat14", "gull14", "osprey40", "aardvark7", "hedgehog60", "gull53", "dingo37", "spider9", "bison80",
            "crocodile79", "gar13", "unicorn59", "aardvark11", "beetle15", "osprey33", "armadillo98", "bluebird4",
            "flea54", "xiphias16", "caiman47", "dingo27", "rat43", "bluebird5", "dingo71", "bear65", "buzzard34",
            "alpaca74", "crocodile59", "rat72", "cow68", "bat58", "jackal57", "ibis19", "squid20", "llama89",
            "woodpecker89", "xiphias0", "camel57", "leopard33", "piranha82", "cat85", "vicuna96", "octopus78",
            "anole75", "elk61", "dingo0", "bat22", "anole82", "buzzard20", "buzzard57", "eagle75", "cavy33", "octopus4",
            "eel26", "hedgehog63", "vulture69", "eland96", "woodpecker89", "fly75", "beetle36", "elk2", "auk21",
            "jaguar17", "cassowary38", "cat51", "bat89", "bass55", "anole72", "zebra40", "cavy28", "beetle55",
            "eland12", "cassowary23", "bison33", "deer56", "dingo73", "salamander12", "bluebird14", "buzzard12",
            "guanaco4", "tern72", "cavy54", "guanaco65", "dove93", "toad28", "dodo27", "vole31", "owl73", "quokka24",
            "bluebird45", "lamprey51", "deer56", "beetle68", "jackal53", "piranha82", "emu42", "camel97", "guanaco54",
            "caiman9", "seal78", "deer69", "sheep52", "cavy88", "caiman36", "camel79", "hyena66", "osprey83",
            "caiman73", "hyena1", "bluebird29", "gnat5", "gull88", "anole76", "piranha15", "walrus81", "mule27",
            "kangaroo99", "hedgehog6", "elk83", "eel96", "eland54", "bluebird28", "aardwolf55", "nutria80",
            "elephant58", "deer15", "cat44", "bison83", "deer33", "cat38", "bison68", "tortoise9", "caiman46", "dove58",
            "crab1", "seal8", "hawk95", "lemur99", "quagga64", "dodo94", "elk48", "cat25", "bass6", "aardvark90",
            "beetle53", "jackal62", "hawk19", "owl28", "buffalo53", "zebra6", "dingo84", "caiman43", "caiman85",
            "deer44", "aardvark70", "giraffe3", "bison91", "flea41", "auk85", "camel83", "deer91", "alpaca37",
            "elephant63", "skink77", "cat29", "gnu57", "gnu0", "bluebird57", "mule54", "eland16", "dodo19", "owl76",
            "walrus77", "crab93", "unicorn40", "kangaroo26", "ray50", "puffin34", "aardwolf32", "auk51", "guanaco21",
            "gharial4", "sidewinder99", "cat52", "dove61", "aardvark19", "cassowary61", "dodo57", "guanaco12", "hyena1",
            "llama48", "guanaco56", "giraffe47", "camel59", "newt7", "tiger85", "leopard28", "dog39", "bear95",
            "quail95", "alpaca56", "dingo2", "beetle43", "buffalo44", "beetle40", "blackbird33", "crocodile97", "newt8",
            "tortoise67", "cow19", "shark24", "dingo65", "cat20", "bison63", "bison50", "cassowary48", "cow98",
            "cassowary64", "beetle27", "finch67", "finch93", "elephant47", "vicuna25", "mosquito32", "bear82", "bat28",
            "woodpecker59", "aardvark29", "hyena84", "fly81", "auk36", "fly86", "armadillo49", "cassowary27",
            "nutria72", "quail61", "caiman92", "toad15", "dodo58", "cassowary64", "bat0", "anole73", "hyena30",
            "flea57", "beetle66", "llama1", "hedgehog59", "cow26", "leopard29", "salamander79", "gar49", "ray80",
            "buzzard38", "eel92", "dingo52", "ostrich48", "deer23", "camel44", "crab44", "gnu34", "blackbird97",
            "anole23", "dog13", "aardwolf18", "woodpecker82", "gull49", "xiphias59", "buffalo67", "warthog58",
            "cassowary53", "camel80", "mosquito54", "alpaca94", "lamprey97", "iguana3", "rat72", "dingo31", "crab87",
            "dodo9", "turtle88", "cat57", "alpaca29", "hawk42", "aardwolf85", "caiman15", "cat0", "worm69", "auk36",
            "deer15", "bear12", "eagle29", "cat83", "bass27", "ostrich60", "zebra81", "tortoise6", "bluebird2", "fly51",
            "unicorn4", "dog11", "bat2", "anole4", "crocodile61", "toad77", "osprey64", "hawk1", "gharial58", "gar4",
            "armadillo62", "terrapin28", "dodo27", "aardwolf77", "cavy54", "blackbird4", "bison27", "bear5",
            "crocodile19", "gnat95", "pelican80", "yak56", "warthog40", "finch74", "eagle95", "cavy9", "alpaca65",
            "bluebird53", "quail39", "beetle80", "eland2", "eland57", "flea98", "cassowary12", "kangaroo84",
            "aardwolf7", "quail31", "camel19", "turtle26", "eel35", "elk0", "anole80", "ant33", "gar14", "finch27",
            "warthog68", "cavy88", "bison66", "armadillo92", "crab49", "buffalo79", "blackbird16", "bass45", "flea0",
            "cavy64", "narwhal20", "osprey14", "tiger86", "crocodile20", "dove89", "spider97", "tiger59", "deer92",
            "bluebird15", "alpaca14", "kangaroo23", "bluebird71", "gnat63", "woodpecker69", "cat34", "crocodile33",
            "buffalo58", "crocodile40", "crab41", "auk26", "shark54", "bass48", "jaguar95", "crocodile64", "finch58",
            "octopus44", "toad67", "deer43", "tern0", "deer4", "dingo92", "giraffe43", "lemur81", "buffalo65",
            "lamprey89", "eagle8", "iguana33", "beetle86", "flea2", "anole22", "bear14", "eagle75", "blackbird19",
            "crab1", "crab56", "iguana82", "bluebird83", "owl40", "octopus50", "leopard46", "alpaca57", "skink50",
            "bison83", "cassowary1", "ibis76", "zebra45", "quagga20", "buffalo11", "cavy10", "aardvark99", "finch50",
            "cavy45", "alpaca44", "manatee63", "warthog73", "crab32", "jaguar4", "buzzard69", "buzzard17", "jackal53",
            "yak66", "buffalo48", "cassowary80", "bass54", "warthog32", "cat20", "jaguar89", "emu20", "ibis10",
            "cavy15", "crocodile95", "gharial48", "dove64", "pelican68", "cavy54", "quagga68", "bear53", "sheep20",
            "mule16", "caiman90", "giraffe71", "dog26", "camel84", "tern52", "lemur60", "elephant70", "cat13",
            "aardvark68", "cat47", "cow10", "woodpecker8", "flea68", "dove20", "armadillo12", "fly87", "crocodile62",
            "cow27", "owl8", "bear47", "pelican13", "jackal88", "elephant82", "blackbird86", "alpaca12", "aardwolf49",
            "deer70", "vole51", "cat60", "bear48", "hedgehog23", "elephant42", "unicorn41", "camel9", "rat41",
            "bluebird67", "armadillo94", "toad95", "bison87", "bluebird35", "bass89", "dove90", "anole33",
            "blackbird48", "bat46", "spider22", "mosquito48", "flea54", "cassowary16", "squid0", "crocodile51",
            "skate70", "vole19", "crocodile47", "blackbird46", "auk9", "osprey74", "dove36", "jackal18", "warthog22",
            "gnu33", "cat54", "deer36", "cassowary82", "camel15", "pelican37", "hyena24", "bluebird86", "bison93",
            "walrus39", "aardwolf54", "anole77", "auk46", "rat42", "elephant60", "octopus38", "gharial43", "caiman9",
            "ant55", "pelican73", "pigeon88", "guanaco89", "elk55", "bison61", "elk13", "anole52", "ibis18", "crab94",
            "gnat47", "deer18", "cassowary13", "hyena14", "snake39", "bat47", "crab86", "llama77", "sheep1", "walrus49",
            "bass67", "bison99", "giraffe86", "cavy18", "dingo52", "squid73", "finch27", "cassowary43", "walrus3",
            "bass65", "crab7", "ant41", "bison13", "bat44", "gnat88", "dog4", "tern82", "gharial5", "blackbird43",
            "emu54", "elephant22", "penguin0", "elephant74", "eland70", "quail6", "spider12", "cassowary66", "gharial7",
            "dingo54", "mule26", "dove46", "cat16", "cavy30", "bass71", "leopard21", "alpaca33", "gnu80", "dog73",
            "owl78", "aardwolf8", "jaguar20", "mosquito11", "bison14", "ibis62", "dodo87", "deer3", "toad35",
            "guanaco5", "jackal33", "llama57", "aardvark44", "shark19", "bat39", "aardvark67", "cow72", "unicorn72",
            "dove75", "eland63", "bluebird49", "skate26", "cat95", "dingo77", "bear78", "newt99", "eel8", "aardvark45",
            "gnat79", "alpaca80", "tern34", "lemur92", "cat52", "hedgehog85", "quail10", "bluebird61", "mosquito4",
            "owl68", "cavy36", "cavy39", "buffalo3", "gar11", "hyena95", "shark19", "hedgehog14", "gnu38",
            "cassowary84", "sheep91", "lamprey61", "buffalo69", "gull22", "eland28", "gharial10", "spider87", "cat41",
    };
    */

    String[] animals = {
            "aardvark", "aardwolf", "alpaca", "anole", "ant", "armadillo", "auk", "bass", "bat", "bear", "beetle",
            "bison", "blackbird", "bluebird", "buffalo", "buzzard", "caiman", "camel", "cassowary", "cat", "cavy",
            "cow", "crab", "crocodile", "deer", "dingo", "dodo", "dog", "dove", "eagle", "eel", "eland", "elephant",
            "elk", "emu", "finch", "flea", "fly", "gar", "gharial", "giraffe", "gnat", "gnu", "guanaco", "gull", "hawk",
            "hedgehog", "hyena", "ibis", "iguana", "jackal", "jaguar", "kangaroo", "kiwi", "koala", "lamprey", "lemur",
            "leopard", "llama", "manatee", "mosquito", "mule", "narwhal", "newt", "nutria", "octopus", "osprey",
            "ostrich", "owl", "pelican", "penguin", "pigeon", "piranha", "puffin", "quagga", "quail", "quokka", "rat",
            "ray", "salamander", "seal", "shark", "sheep", "sidewinder", "skate", "skink", "snake", "spider", "squid",
            "tern", "terrapin", "tiger", "toad", "tortoise", "turtle", "unicorn", "vicuna", "vole", "vulture", "walrus",
            "warthog", "woodpecker", "worm", "xiphias", "yak", "zebra"
    };

    public void testAddsDeletesRandom() {

        //       t = new BASet<String>(String.CASE_INSENSITIVE_ORDER);
        //       int n_adds = adds.length;
        //       for (int i = 0; i < n_adds-1; i++) {
        //           t.add(adds[i]);
        //       }
        //       t.add(adds[n_adds-1]);


        String[] strings = new String[10000];
        int n = 0;

        Random r = new Random(0x12345555);
        int l = animals.length;

        for (int j = 0; j < 10000; j++) {

            t = new BASet<String>(String.CASE_INSENSITIVE_ORDER);

            try {

                for (int k = 0; k < 10000; k++) {
                    int x = r.nextInt(l);
                    int y = r.nextInt(l / 2);
                    int z = r.nextInt(l / 4);
                    int choice = r.nextInt(4);
                    String s;
                    int suffix = r.nextInt(100);
                    if (choice == 2) {
                        s = (animals[z] + suffix);
                    } else if (choice == 1) {
                        s = (animals[y] + suffix);
                    } else {
                        s = (animals[x] + suffix);
                    }
                    strings[k] = s;
                    n = k;
                    t.add(s);
                }

                t.ok();

            }
            catch (Error e) {

                System.err.println("Failed, at length = " + n + ", iteration = " + j);

                //                System.out.println("Strings[] adds = {");
                //                for (int i = 0; i <= n; i++) {
                //                    System.out.println("\""+strings[i]+"\",");
                //                }
                //                System.out.println("};");

                //                t = new BASet<String>(String.CASE_INSENSITIVE_ORDER);
                //                for (int i = 0; i < n; i++) {
                //                    t.add(strings[i]);
                //                }
                //                t.add(strings[n]);
            }
        }
    }

}
