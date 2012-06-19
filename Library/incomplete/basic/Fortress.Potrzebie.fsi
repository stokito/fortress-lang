(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

import { Length, Volume, Mass, Time, Force, Energy, Power, Temperature, Angle,
         millimeters, kilogram, liter } from Fortress.SIunits

(* Reference: Donald Knuth, The Potrzebie System of Weights and Measures,
   MAD #33 (June 1957), pp. 36--37.
   Reprinted in "Like, Mad". *)

farshimmelt : QQ64
furshlugginer : ZZ64

unit potrzebie potrzebies p_: Length (* thickness of MAD #26 *)
unit fp_: Length
unit millipotrzebie millipotrzebies mp_: Length
unit centipotrzebie centipotrzebies cp_: Length
unit decipotrzebie decipotrzebies dp_: Length
unit dekapotrzebie dekapotrzebies Dp_: Length
unit hectopotrzebie hectopotrzebies Hp_: Length
unit kilopotrzebie kilopotrzebies Kp_: Length
unit Fp_: Length

unit ngogn ngogns n_: Volume
unit fn_: Volume
unit millingogn millingogns mn_: Volume
unit centingogn centingogns cn_: Volume
unit decingogn decingogns dn_: Volume
unit dekangogn dekangogns Dn_: Volume
unit hectongogn hectongogns Hn_: Volume
unit kilongogn kilongogns Kn_: Volume
unit Fn_: Volume

(* According to the reference:  Halavah, of course, is a form of "pie",
   and it has a specific gravity of 3.1416 and a specific heat of 0.31416.

   We assume the specific gravity of water is 1, and that the mass of
   1 liter of water is 1 kilogram, so the mass of 1 ngogn of halavah
   is 3.1416 (1 kilogram/liter) (liter/ngogn) (1 ngogn).
*)

unit blintz blintzes b_: Mass
unit fb_: Mass
unit milliblintz milliblintzes mb_: Mass
unit centiblintz centiblintzes cb_: Mass
unit deciblintz deciblintzes db_: Mass
unit dekablintz dekablintzes Db_: Mass
unit hectoblintz hectoblintzes Hb_: Mass
unit kiloblintz kiloblintzes Kb_: Mass
unit Fb_: Mass

(* There seem to be some typos in the definition of time units in the original
   publication.  We assume that the factors of 1000 or 100 in the definitions
   of kovac, martin, and wood should have been 10. *)

unit clarke clarkes cl_: Time
unit wolverton wolvertons wl_: Time
unit kovac kovacs kv_: Time
unit martin martins mn_: Time
unit wood woods wd_: Time
unit mingo mingos mi_: Time
unit cowznofski cowznofskis cow_: Time

unit blintzal blintzals b_al_: Force
unit fb_al_: Force
unit milliblintzal milliblintzals mb_al_: Force
unit centiblintzal centiblintzals cb_al_: Force
unit deciblintzal deciblintzals db_al_: Force
unit dekablintzal dekablintzals Db_al_: Force
unit hectoblintzal hectoblintzals Hb_al_: Force
unit kiloblintzal kiloblintzals Kb_al_: Force
unit Fb_al_: Force

(* To conform to standard style, we change "b-al" to "b_al". *)

unit hoo hoos h_: Energy
unit fh_: Energy
unit millihoo millihoos mh_: Energy
unit centihoo centihoos ch_: Energy
unit decihoo decihoos dh_: Energy
unit dekahoo dekahoos Dh_: Energy
unit hectohoo hectohoos Hh_: Energy
unit kilohoo kilohoos Kh_: Energy
unit Fh_: Energy
unit hah hahs hh_: Energy

unit whatmeworry WMW_: Power
unit fWMW_: Energy = farshimmelt whatmeworry
unit milliwhatmeworry milliwhatmeworrys mWMW_: Energy
unit centiwhatmeworry centiwhatmeworrys cWMW_: Energy
unit deciwhatmeworry deciwhatmeworrys dWMW_: Energy
unit dekawhatmeworry dekawhatmeworrys DWMW_: Energy
unit hectowhatmeworry hectowhatmeworrys HWMW_: Energy
unit kilowhatmeworry kilowhatmeworrys KWMW_: Energy
unit FWMW_: Energy

(* To conform to standard style, we change "A.P." to "AP". *)

unit aeolipower AP_: Power

(* We assume that an absolute temperature of 0 degrees Smurdley is the same as
   an absolute temperature of 0 degrees Celsius.  Then a relative temperature
   of 1 degree Smurdley is 27/100 degree Celsius = 0.27 kelvin. *)

unit degreeSmurdley: Temperature

unit zygo: Angle
unit zorch: Angle
unit quircit quircits: Angle
