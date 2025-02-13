Method Usage
============

This section describes the usage of the `examplePipelineMethod`.

.. code-block:: groovy

    def examplePipelineMethod(Map args)

Usage Example
-------------

Here are examples of how to use the `examplePipelineMethod` with named parameters:

.. code-block:: groovy

    def result1 = examplePipelineMethod(param1: "Hello", param2: 123)
    println(result1)  # Output: Hello - 123

.. code-block:: groovy

    def result2 = examplePipelineMethod(param1: "World", param2: 456)
    println(result2)  # Output: World - 456

.. code-block:: groovy

    def result = examplePipelineMethod(param1: "Hello", param2: 123)
    println(result)  # Output: Hello - 123
