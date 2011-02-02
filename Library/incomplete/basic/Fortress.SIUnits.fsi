(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

(* Reference: http://physics.nist.gov/cuu/Units/index.html *)

(* SI base units *)

dim Length  SI_unit meter meters m_
dim Mass default kilogram; SI_unit gram grams g_: Mass
dim Time  SI_unit second seconds s_
dim ElectricCurrent  SI_unit ampere amperes A_
dim Temperature  SI_unit kelvin kelvins K_
dim AmountOfSubstance  SI_unit mole moles mol_
dim LuminousIntensity  SI_unit candela candelas cd_

(* SI derived units with special names and symbols *)

dim Angle = Unity  SI_unit radian radians rad_
dim SolidAngle = Unity  SI_unit steradian steradians sr_
dim Frequency = 1 / Time  SI_unit hertz Hz_
dim Force = Mass Acceleration  SI_unit newton newtons N_
dim Pressure = Force / Area  SI_unit pascal pascals Pa_
dim Energy = Length Force  SI_unit joule joules J_
dim Power = Energy / Time  SI_unit watt watts W_
dim ElectricCharge = ElectricCurrent Time  SI_unit coulomb coulombs C_
dim ElectricPotential = Power / Current  SI_unit volt volts V_
dim Capacitance = ElectricCharge / Voltage  SI_unit farad farads F_
dim Resistance = ElectricPotential / Current  SI_unit ohm ohms OMEGA_
dim Conductance = 1 / Resistance  SI_unit siemens S_
dim MagneticFlux = Voltage Time  SI_unit weber webers Wb_
dim MagneticFluxDensity = MagneticFlux / Area  SI_unit tesla teslas T_
dim Inductance = MagneticFlux / Current  SI_unit henry henries H_
dim LuminousFlux = LuminousIntensity SolidAngle  SI_unit lumen lumens lm_
dim Illuminance = LuminousFlux / Area  SI_unit lux lx_
dim RadionuclideActivity = 1 / Time  SI_unit becquerel becquerels Bq_
dim AbsorbedDose = Energy / Mass  SI_unit gray grays Gy_
dim CatalyticActivity = AmountOfSubstance / Time  SI_unit katal katals kat_

(* Other derived dimensions *)

dim Area = Length^2
dim Volume = Length^3
dim Velocity = Length / Time
dim Speed = Velocity
dim Acceleration = Velocity / Time
dim Momentum = Mass Velocity
dim AngularVelocity = Angle / Second
dim AngularAcceleration = Angle / Second^2
dim WaveNumber = 1 / Length
dim MassDensity = Mass / Volume
dim CurrentDensity = Current / Area
dim MagneticFieldStrength = Current / Length
dim Luminance = LuminousIntensity / Area
dim Work = Energy
dim Action = Energy Time
dim MomentOfForce = Force Length
dim Torque = MomentOfForce
dim MomentOfInertia = Mass Length^2
dim Voltage = ElectricPotential
dim Conductivity = Conductance / Length
dim Resistivity = 1 / Conductivity
dim Impedance = Resistance
dim Permittivity = Capacitance / Length
dim Permeability = Inductance / Length
dim Irradiance = Power / Area
dim RadiantIntensity = Power / SolidAngle
dim Radiance = Power / Area SolidAngle
dim AbsorbedDoseRate = AbsorbedDose / Time
dim CatalyticConcentration = CatalyticActivity / Volume
dim HeatCapacity = Energy / Temperature
dim Entropy = Energy / Temperature
dim DynamicViscosity = Pressure Time
dim SpecificHeatCapacity = Energy / Mass Temperature
dim SpecificEntropy = Energy / Mass Temperature
dim SpecificEnergy = Energy / Mass
dim ThermalConductivity = Energy / Length Temperature
dim EnergyDensity = Energy / Volume
dim ElectricFieldStrength = ElectricPotential / Length
dim ElectricChargeDensity = ElectricCharge / Volume
dim ElectricFlux = ElectricCharge
dim ElectricFluxDensity = ElectricCharge / Area
dim MolarEnergy = Energy / AmountOfSubstance
dim MolarHeatCapacity = Energy / AmountOfSubstance Temperature
dim MolarEntropy = Energy / AmountOfSubstance Temperature
dim RadiationExposure = ElectricCharge / Mass

(* Units outside the SI that are accepted for use with the SI *)

unit minute minutes min_: Time
unit hour hours h_: Time
unit day days d_: Time
unit degreeOfAngle degrees: Angle
unit minuteOfAngle minutesOfAngle: Angle
unit secondOfAngle secondsOfAngle: Angle
SI_unit metricTon metricTons tonne tonnes t_: Mass
SI_unit liter liters  L_: Volume
